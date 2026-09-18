package com.v2ray.ang.enums

/** How the SSH client authenticates to the tunnel server. */
enum class SshAuthMode(val type: String) {
    PASSWORD("password"),
    PRIVATE_KEY("key");

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: PASSWORD
    }
}
