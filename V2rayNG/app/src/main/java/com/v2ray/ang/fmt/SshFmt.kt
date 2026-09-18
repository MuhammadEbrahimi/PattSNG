package com.v2ray.ang.fmt

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.SshAuthMode
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.util.Utils
import java.net.URI

/**
 * `ssh://user[:password]@host[:port]?auth=password|key&hostkey=..&keepalive=..&compression=1#remark`
 *
 * The port is optional in a link and falls back to 22, the way any SSH client reads a bare
 * `ssh://host`. A private key is too long for a shareable link, so a key profile carries only its
 * auth mode in the link and the key itself stays on the device.
 */
object SshFmt : FmtBase() {

    enum class Problem {
        MISSING_HOST,
        INVALID_PORT,
        MISSING_USERNAME,
        MISSING_PASSWORD,
        MISSING_KEY,
    }

    fun parse(str: String): ProfileItem? {
        val config = ProfileItem.create(EConfigType.SSH)

        val uri = URI(Utils.fixIllegalUrl(str))
        val queryParam = if (uri.rawQuery.isNullOrEmpty()) emptyMap() else getQueryParam(uri)

        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "SSH" }
        config.server = uri.idnHost.nullIfBlank() ?: return null
        config.serverPort = uri.port.takeIf { it in 1..65535 }?.toString()
            ?: AppConfig.SSH_DEFAULT_PORT.toString()

        val userInfo = Utils.decodeURIComponent(uri.userInfo.orEmpty())
        val separator = userInfo.indexOf(':')
        if (separator >= 0) {
            config.sshUsername = userInfo.substring(0, separator).nullIfBlank()
            config.password = userInfo.substring(separator + 1).nullIfBlank()
        } else {
            config.sshUsername = userInfo.nullIfBlank()
        }

        config.sshAuthMode = SshAuthMode.fromString(queryParam["auth"]).type
        config.sshHostKey = queryParam["hostkey"]?.nullIfBlank()
        config.sshKeepAlive = queryParam["keepalive"]?.trim()?.toIntOrNull()?.toString()
        config.sshCompression = queryParam["compression"] == "1"

        return config
    }

    fun toUri(config: ProfileItem): String {
        val mode = SshAuthMode.fromString(config.sshAuthMode)
        val query = linkedMapOf("auth" to mode.type)
        config.sshHostKey?.nullIfBlank()?.let { query["hostkey"] = it }
        config.sshKeepAlive?.nullIfBlank()?.let { query["keepalive"] = it }
        if (config.sshCompression == true) query["compression"] = "1"

        val userInfo = buildString {
            append(config.sshUsername.orEmpty())
            if (mode == SshAuthMode.PASSWORD && !config.password.isNullOrEmpty()) {
                append(':')
                append(config.password)
            }
        }

        return toUri(config, userInfo, HashMap(query))
    }

    /**
     * Trims the profile into the shape the tunnel is started with and reports the first field that
     * would keep it from connecting.
     */
    fun normalize(config: ProfileItem): Problem? {
        val host = config.server?.trim().orEmpty()
        if (host.isEmpty()) return Problem.MISSING_HOST
        config.server = host

        val portText = config.serverPort?.trim().orEmpty()
        val port = if (portText.isEmpty()) {
            AppConfig.SSH_DEFAULT_PORT
        } else {
            portText.toIntOrNull() ?: return Problem.INVALID_PORT
        }
        if (port !in 1..65535) return Problem.INVALID_PORT
        config.serverPort = port.toString()

        val username = config.sshUsername?.trim().orEmpty()
        if (username.isEmpty()) return Problem.MISSING_USERNAME
        config.sshUsername = username

        when (SshAuthMode.fromString(config.sshAuthMode)) {
            SshAuthMode.PASSWORD -> {
                if (config.password.isNullOrEmpty()) return Problem.MISSING_PASSWORD
                config.sshPrivateKey = null
                config.sshPassphrase = null
            }

            SshAuthMode.PRIVATE_KEY -> {
                if (config.sshPrivateKey?.trim().isNullOrEmpty()) return Problem.MISSING_KEY
                config.sshPrivateKey = config.sshPrivateKey?.trim()
                config.password = null
            }
        }

        config.sshHostKey = config.sshHostKey?.trim()?.nullIfBlank()
        config.sshKeepAlive = config.sshKeepAlive?.trim()?.toIntOrNull()?.coerceIn(0, 600)?.toString()
        return null
    }
}
