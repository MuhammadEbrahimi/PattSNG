package com.v2ray.ang.fmt

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.SshAuthMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SshFmtTest {

    private fun profile(
        server: String? = "tunnel.example.com",
        port: String? = "2222",
        username: String? = "ara",
        password: String? = "secret",
        authMode: SshAuthMode = SshAuthMode.PASSWORD,
        privateKey: String? = null,
        keepAlive: String? = null,
    ) = ProfileItem(
        configType = EConfigType.SSH,
        remarks = "test",
        server = server,
        serverPort = port,
        password = password,
        sshUsername = username,
        sshAuthMode = authMode.type,
        sshPrivateKey = privateKey,
        sshKeepAlive = keepAlive,
    )

    @Test
    fun `parse reads user password host and port`() {
        val config = SshFmt.parse("ssh://ara:secret@tunnel.example.com:2222#Home")

        assertNotNull(config)
        assertEquals("Home", config!!.remarks)
        assertEquals("tunnel.example.com", config.server)
        assertEquals("2222", config.serverPort)
        assertEquals("ara", config.sshUsername)
        assertEquals("secret", config.password)
        assertEquals(SshAuthMode.PASSWORD.type, config.sshAuthMode)
    }

    @Test
    fun `parse without a port falls back to 22`() {
        val config = SshFmt.parse("ssh://ara@tunnel.example.com")

        assertEquals("22", config?.serverPort)
        assertEquals("ara", config?.sshUsername)
        assertNull(config?.password)
    }

    @Test
    fun `parse reads the key auth mode and the options`() {
        val config = SshFmt.parse("ssh://ara@1.2.3.4:22?auth=key&keepalive=45&compression=1#Key")

        assertEquals(SshAuthMode.PRIVATE_KEY.type, config?.sshAuthMode)
        assertEquals("45", config?.sshKeepAlive)
        assertEquals(true, config?.sshCompression)
    }

    @Test
    fun `toUri keeps the profile readable by parse`() {
        val original = profile(keepAlive = "60")

        val reparsed = SshFmt.parse(SshFmt.toUri(original))

        assertEquals(original.server, reparsed?.server)
        assertEquals(original.serverPort, reparsed?.serverPort)
        assertEquals(original.sshUsername, reparsed?.sshUsername)
        assertEquals(original.password, reparsed?.password)
        assertEquals("60", reparsed?.sshKeepAlive)
    }

    @Test
    fun `toUri leaves the private key on the device`() {
        val uri = SshFmt.toUri(
            profile(authMode = SshAuthMode.PRIVATE_KEY, password = null, privateKey = "-----BEGIN-----")
        )

        assertTrue(uri.contains("auth=key"))
        assertTrue(!uri.contains("BEGIN"))
    }

    @Test
    fun `normalize accepts a complete password profile`() {
        val config = profile(port = " 2222 ", username = " ara ")

        assertNull(SshFmt.normalize(config))
        assertEquals("2222", config.serverPort)
        assertEquals("ara", config.sshUsername)
    }

    @Test
    fun `normalize fills an empty port with 22`() {
        val config = profile(port = "")

        assertNull(SshFmt.normalize(config))
        assertEquals("22", config.serverPort)
    }

    @Test
    fun `normalize reports the first unusable field`() {
        assertEquals(SshFmt.Problem.MISSING_HOST, SshFmt.normalize(profile(server = " ")))
        assertEquals(SshFmt.Problem.INVALID_PORT, SshFmt.normalize(profile(port = "70000")))
        assertEquals(SshFmt.Problem.MISSING_USERNAME, SshFmt.normalize(profile(username = null)))
        assertEquals(SshFmt.Problem.MISSING_PASSWORD, SshFmt.normalize(profile(password = null)))
        assertEquals(
            SshFmt.Problem.MISSING_KEY,
            SshFmt.normalize(profile(authMode = SshAuthMode.PRIVATE_KEY, privateKey = "  "))
        )
    }

    @Test
    fun `normalize drops the credential the auth mode does not use`() {
        val keyProfile = profile(authMode = SshAuthMode.PRIVATE_KEY, privateKey = "-----BEGIN-----")
        assertNull(SshFmt.normalize(keyProfile))
        assertNull(keyProfile.password)

        val passwordProfile = profile(privateKey = "-----BEGIN-----")
        assertNull(SshFmt.normalize(passwordProfile))
        assertNull(passwordProfile.sshPrivateKey)
    }
}
