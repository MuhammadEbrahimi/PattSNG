package com.v2ray.ang.core

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SshCoreManagerTest {

    private fun profile(port: String? = "2222", keepAlive: String? = null) = ProfileItem(
        configType = EConfigType.SSH,
        remarks = "test",
        server = "tunnel.example.com",
        serverPort = port,
        sshUsername = "ara",
        password = "secret",
        sshKeepAlive = keepAlive,
    )

    @Test
    fun `server port uses the profile value`() {
        assertEquals(2222, SshCoreManager.serverPort(profile()))
    }

    @Test
    fun `server port falls back to 22 when unusable`() {
        assertEquals(22, SshCoreManager.serverPort(profile(port = null)))
        assertEquals(22, SshCoreManager.serverPort(profile(port = "")))
        assertEquals(22, SshCoreManager.serverPort(profile(port = "ssh")))
        assertEquals(22, SshCoreManager.serverPort(profile(port = "0")))
        assertEquals(22, SshCoreManager.serverPort(profile(port = "70000")))
    }

    @Test
    fun `keep alive is disabled by a non positive value and capped otherwise`() {
        assertEquals(30_000, SshCoreManager.keepAliveMs(profile()))
        assertEquals(45_000, SshCoreManager.keepAliveMs(profile(keepAlive = "45")))
        assertEquals(0, SshCoreManager.keepAliveMs(profile(keepAlive = "0")))
        assertEquals(0, SshCoreManager.keepAliveMs(profile(keepAlive = "-5")))
        assertEquals(600_000, SshCoreManager.keepAliveMs(profile(keepAlive = "9000")))
    }

    @Test
    fun `a profile without a pinned host key trusts the presented one`() {
        assertTrue(SshCoreManager.acceptsHostKey(null, "aa:bb:cc"))
        assertTrue(SshCoreManager.acceptsHostKey("  ", "aa:bb:cc"))
    }

    @Test
    fun `a pinned host key is compared ignoring case prefix and separators`() {
        assertTrue(SshCoreManager.acceptsHostKey("aa:bb:cc", "AA:BB:CC"))
        assertTrue(SshCoreManager.acceptsHostKey("MD5:aa:bb:cc", "aa:bb:cc"))
        assertTrue(SshCoreManager.acceptsHostKey("aabbcc", "aa:bb:cc"))
    }

    @Test
    fun `a pinned host key rejects a different or missing key`() {
        assertFalse(SshCoreManager.acceptsHostKey("aa:bb:cc", "dd:ee:ff"))
        assertFalse(SshCoreManager.acceptsHostKey("aa:bb:cc", null))
        assertFalse(SshCoreManager.acceptsHostKey("aa:bb:cc", ""))
    }

    @Test
    fun `readiness reports as soon as the listener accepts`() = runBlocking {
        var polls = 0
        val ready = SshCoreManager.awaitReady(
            timeoutMs = 1000,
            pollMs = 1,
            running = { true },
            listening = { ++polls >= 3 },
        )

        assertTrue(ready)
        assertEquals(3, polls)
    }

    @Test
    fun `readiness stops waiting once the session is gone`() = runBlocking {
        val ready = SshCoreManager.awaitReady(
            timeoutMs = 1000,
            pollMs = 1,
            running = { false },
            listening = { throw AssertionError("must not probe a dead session") },
        )

        assertFalse(ready)
    }

    @Test
    fun `readiness gives up at the timeout`() = runBlocking {
        val ready = SshCoreManager.awaitReady(
            timeoutMs = 30,
            pollMs = 1,
            running = { true },
            listening = { false },
        )

        assertFalse(ready)
    }
}
