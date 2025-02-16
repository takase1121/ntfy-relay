// Copyright (C) 2025 by Takase

package it.eja.ntfyrelay

data class NotificationServerConfig(
    val url: String,
    val authType: AuthType,
    val username: String?,
    val password: String?
) {
    enum class AuthType {
        HTTP_NONE,
        HTTP_BASIC,
        HTTP_TOKEN
    }
}
