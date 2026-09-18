package com.v2ray.ang.core

import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Logger
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.SshAuthMode
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import com.jcraft.jsch.Session as JschSession

/**
 * Owns the SSH tunnel of the daemon: one live session at a time, a dynamic (`ssh -D`) SOCKS5
 * listener on loopback, an exit callback the service reacts to, library output relayed into the
 * app log, and readiness probing.
 *
 * This is the in-process counterpart of [AetherCoreManager]: the tunnel is a JSch session inside
 * the app process rather than a native core process, so there is no binary, no `/proc` scanning
 * and no stale-process reaping. Everything the service depends on keeps the same shape, which is
 * why `CoreServiceManager` drives the two managers the same way.
 *
 * The socket needs no [android.net.VpnService.protect] call: `CoreVpnService` puts this package
 * in `addDisallowedApplication`, so app-process traffic never enters the tun device, and the
 * proxy-only and root modes have no tun for it to loop through either.
 */
object SshCoreManager {

    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val PROBE_TIMEOUT_MS = 1000
    private const val READY_POLL_MS = 300L
    private const val ALIVE_POLL_MS = 2000L
    private const val DEFAULT_KEEP_ALIVE_MS = 30_000
    private const val KEY_IDENTITY = "pattng-ssh"

    private val lifecycle = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ssh-core").apply { isDaemon = true }
    }

    @Volatile
    private var session: Session? = null
    @Volatile
    private var socksServer: SshSocksServer? = null

    val socksPort: Int get() = AppConfig.PORT_SSH_SOCKS.toInt()

    val isRunning: Boolean get() = session?.jsch?.isConnected == true

    /** Unlike the Aether core, the tunnel is pure Java and runs on every ABI the app ships. */
    fun isSupported(context: Context): Boolean = true

    @Synchronized
    fun start(context: Context, profile: ProfileItem, onExit: () -> Unit) {
        stop()
        val next = Session(onExit)
        session = next
        val snapshot = profile.copy()
        lifecycle.execute { open(next, snapshot) }
    }

    @Synchronized
    fun stop() {
        val current = session ?: return
        session = null
        current.stopped = true
        lifecycle.execute {
            stopSocks()
            current.jsch?.disconnect()
        }
    }

    suspend fun awaitListening(timeoutMs: Long): Boolean =
        awaitReady(timeoutMs, READY_POLL_MS, { session != null }, { acceptsConnections(socksPort) })

    internal suspend fun awaitReady(
        timeoutMs: Long,
        pollMs: Long,
        running: () -> Boolean,
        listening: () -> Boolean,
    ): Boolean = withTimeoutOrNull(timeoutMs) {
        while (running()) {
            if (withContext(Dispatchers.IO) { listening() }) return@withTimeoutOrNull true
            delay(pollMs)
        }
        false
    } ?: false

    internal fun acceptsConnections(port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(AppConfig.LOOPBACK, port), PROBE_TIMEOUT_MS) }
        true
    } catch (_: IOException) {
        false
    }

    /**
     * The port the tunnel dials on the server. A blank or unusable value falls back to 22, so a
     * profile saved or imported without a port connects the way `ssh host` would.
     */
    internal fun serverPort(profile: ProfileItem): Int =
        profile.serverPort?.trim()?.toIntOrNull()?.takeIf { it in 1..65535 } ?: AppConfig.SSH_DEFAULT_PORT

    /** Keep-alive interval in milliseconds; 0 disables the probes. */
    internal fun keepAliveMs(profile: ProfileItem): Int {
        val seconds = profile.sshKeepAlive?.trim()?.toIntOrNull() ?: return DEFAULT_KEEP_ALIVE_MS
        return if (seconds <= 0) 0 else seconds.coerceAtMost(600) * 1000
    }

    /**
     * True when the key with fingerprint [actual] is the one the profile pins. A profile without a
     * pinned key trusts the first key it sees, which is the exposure of `StrictHostKeyChecking=no`;
     * pinning one turns every later connection into a check.
     */
    internal fun acceptsHostKey(expected: String?, actual: String?): Boolean {
        val pinned = expected?.trim()?.removePrefix("MD5:")?.removePrefix("SHA256:").orEmpty()
        if (pinned.isEmpty()) return true
        val presented = actual?.trim().orEmpty()
        if (presented.isEmpty()) return false
        return pinned.equals(presented, ignoreCase = true) ||
            pinned.replace(":", "").equals(presented.replace(":", ""), ignoreCase = true)
    }

    /**
     * Builds the unconnected JSch session for [profile]. Kept apart from [open] so credentials,
     * algorithms and timeouts can be asserted without a server.
     */
    @Throws(JSchException::class)
    internal fun buildSession(profile: ProfileItem): JschSession {
        val jsch = JSch()
        val mode = SshAuthMode.fromString(profile.sshAuthMode)
        val username = profile.sshUsername?.trim().orEmpty().ifEmpty { "root" }

        if (mode == SshAuthMode.PRIVATE_KEY) {
            val key = profile.sshPrivateKey?.trim().orEmpty()
            if (key.isEmpty()) throw JSchException("SSH: the profile carries no private key")
            jsch.addIdentity(
                KEY_IDENTITY,
                key.toByteArray(Charsets.UTF_8),
                null,
                profile.sshPassphrase?.takeUnless { it.isBlank() }?.toByteArray(Charsets.UTF_8),
            )
        }

        val jschSession = jsch.getSession(username, profile.server?.trim().orEmpty(), serverPort(profile))
        if (mode == SshAuthMode.PASSWORD) {
            jschSession.setPassword(profile.password.orEmpty())
        }
        jschSession.setConfig(
            "PreferredAuthentications",
            if (mode == SshAuthMode.PRIVATE_KEY) "publickey" else "password,keyboard-interactive",
        )
        // The key is compared to the pinned fingerprint after the handshake instead, so JSch's
        // unknown-host failure is not in the way of the first connection.
        jschSession.setConfig("StrictHostKeyChecking", "no")
        val compression = if (profile.sshCompression == true) "zlib@openssh.com,zlib,none" else "none"
        jschSession.setConfig("compression.s2c", compression)
        jschSession.setConfig("compression.c2s", compression)
        keepAliveMs(profile).takeIf { it > 0 }?.let {
            jschSession.serverAliveInterval = it
            jschSession.serverAliveCountMax = 3
        }
        jschSession.timeout = CONNECT_TIMEOUT_MS
        return jschSession
    }

    private fun open(target: Session, profile: ProfileItem) {
        if (session !== target) return
        JSch.setLogger(JschRelay)

        val jschSession = try {
            buildSession(profile)
        } catch (e: JSchException) {
            fail(target, "SshCore: failed to prepare the session", e)
            return
        }
        target.jsch = jschSession

        try {
            jschSession.connect(CONNECT_TIMEOUT_MS)
            val fingerprint = jschSession.hostKey?.getFingerPrint(JSch())
            if (!acceptsHostKey(profile.sshHostKey, fingerprint)) {
                jschSession.disconnect()
                fail(target, "SshCore: the server key $fingerprint does not match the pinned one", null)
                return
            }
            socksServer = SshSocksServer(
                session = jschSession,
                port = socksPort,
                bindAddress = AppConfig.LOOPBACK,
                connectTimeoutMs = CONNECT_TIMEOUT_MS,
            ).also { it.start() }
            LogUtil.i(
                AppConfig.TAG,
                "SshCore: tunnel up, SOCKS on ${AppConfig.LOOPBACK}:$socksPort via port ${serverPort(profile)}"
            )
        } catch (e: JSchException) {
            stopSocks()
            jschSession.disconnect()
            fail(target, "SshCore: failed to open the tunnel", e)
            return
        } catch (e: IOException) {
            // The SOCKS listener could not bind, usually because a previous tunnel still holds the
            // port; without it the profile would look connected while carrying no traffic.
            stopSocks()
            jschSession.disconnect()
            fail(target, "SshCore: failed to bind the SOCKS listener on port $socksPort", e)
            return
        }

        thread(name = "ssh-core-watchdog", isDaemon = true) { watch(target, jschSession) }
    }

    private fun watch(target: Session, jschSession: JschSession) {
        while (session === target && jschSession.isConnected) {
            try {
                Thread.sleep(ALIVE_POLL_MS)
            } catch (_: InterruptedException) {
                return
            }
        }
        stopSocks()
        if (target.stopped || !release(target)) return
        LogUtil.e(AppConfig.TAG, "SshCore: the tunnel dropped on its own")
        target.onExit()
    }

    private fun fail(target: Session, message: String, cause: Exception?) {
        if (cause != null) LogUtil.e(AppConfig.TAG, message, cause) else LogUtil.e(AppConfig.TAG, message)
        if (release(target)) target.onExit()
    }

    /** The listener outlives a dropped session, so it is closed on every path that ends a tunnel. */
    private fun stopSocks() {
        socksServer?.stop()
        socksServer = null
    }

    @Synchronized
    private fun release(target: Session): Boolean {
        if (session !== target) return false
        session = null
        return true
    }

    private class Session(val onExit: () -> Unit) {
        var jsch: JschSession? = null

        /** Set when the service asked for the stop, so the watchdog does not report it as a drop. */
        @Volatile
        var stopped = false
    }

    /** Sends the library's own diagnostics to the app log, like the Aether core output relay. */
    private object JschRelay : Logger {
        override fun isEnabled(level: Int): Boolean = true

        override fun log(level: Int, message: String?) {
            val text = message?.trim().orEmpty()
            if (text.isEmpty()) return
            when (level) {
                Logger.FATAL, Logger.ERROR -> LogUtil.e(AppConfig.TAG, "[ssh] $text")
                Logger.WARN -> LogUtil.w(AppConfig.TAG, "[ssh] $text")
                Logger.DEBUG -> LogUtil.d(AppConfig.TAG, "[ssh] $text")
                else -> LogUtil.i(AppConfig.TAG, "[ssh] $text")
            }
        }
    }
}