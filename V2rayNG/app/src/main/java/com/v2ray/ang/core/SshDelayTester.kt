package com.v2ray.ang.core

import com.jcraft.jsch.Session as JschSession
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Delay test for SSH profiles.
 *
 * An SSH profile carries traffic through the loopback SOCKS listener the running tunnel opens, so
 * the speedtest config Xray is handed points at that port. For any profile other than the running
 * one nothing listens there, which is why such a test could only ever report -1. The measurement is
 * therefore done the way the Aether one is: through the live session when the profile is the one
 * running, and through a tunnel of its own otherwise.
 */
object SshDelayTester {

    private const val CONNECT_TIMEOUT_MS = 10_000

    /** Only one temporary tunnel at a time, so a batch test does not open one per row at once. */
    private val tunnels = Mutex()

    suspend fun measure(guid: String, profile: ProfileItem, url: String): Long {
        val activeGuid = MmkvManager.getSelectServer()
        if (guid == activeGuid && SshCoreManager.isRunning) {
            val listening = withContext(Dispatchers.IO) {
                SshCoreManager.acceptsConnections(SshCoreManager.socksPort)
            }
            // The running profile is not a failure before its tunnel is up; it is not measurable yet.
            if (!listening) {
                LogUtil.i(AppConfig.TAG, "SshTest: left untested, the live tunnel is still connecting, guid=$guid")
                return AetherDelayTester.UNTESTED
            }
            return withContext(Dispatchers.IO) {
                AetherDelayTester.requestDelay(SshCoreManager.socksPort, url)
            }
        }
        return tunnels.withLock { throughNewTunnel(guid, profile, url) }
    }

    /**
     * Opens a session of its own on a free loopback port, measures through it and tears it down.
     * The live tunnel, if there is one, keeps its own port and is left untouched.
     */
    private suspend fun throughNewTunnel(guid: String, profile: ProfileItem, url: String): Long =
        withContext(Dispatchers.IO) {
            val port = Utils.findRandomFreePort()
            var session: JschSession? = null
            var socks: SshSocksServer? = null
            try {
                val opened = SshCoreManager.buildSession(profile)
                session = opened
                opened.connect(CONNECT_TIMEOUT_MS)
                val server = SshSocksServer(
                    session = opened,
                    port = port,
                    bindAddress = AppConfig.LOOPBACK,
                    connectTimeoutMs = CONNECT_TIMEOUT_MS,
                )
                socks = server
                server.start()
                val delay = AetherDelayTester.requestDelay(port, url)
                if (delay < 0) LogUtil.w(AppConfig.TAG, "SshTest: no answer through the tunnel, guid=$guid")
                delay
            } catch (e: Exception) {
                LogUtil.w(AppConfig.TAG, "SshTest: the tunnel could not be opened, guid=$guid", e)
                -1L
            } finally {
                socks?.stop()
                session?.disconnect()
            }
        }
}
