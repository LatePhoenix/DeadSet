package com.example.data.remote

import com.example.BuildConfig

/**
 * How the app reaches Ticketmaster Discovery.
 *
 * Release builds should use [Mode.PROXY] (key lives in the Cloudflare Worker).
 * Debug may fall back to a local key in BuildConfig ([Mode.DIRECT]).
 */
data class TicketmasterAccess(
    val mode: Mode,
    /** Retrofit base URL ending with `/`, empty when [Mode.OFF]. */
    val baseUrl: String,
    /** Empty when using the proxy (Worker injects the secret). */
    val apiKey: String
) {
    enum class Mode { PROXY, DIRECT, OFF }

    val isConfigured: Boolean get() = mode != Mode.OFF

    companion object {
        const val DIRECT_BASE_URL = "https://app.ticketmaster.com/"
        private const val PROXY_SENTINEL = "YOUR_TICKETMASTER_PROXY_URL"
        private const val KEY_SENTINEL = "YOUR_TICKETMASTER_API_KEY"

        fun resolve(
            proxyUrl: String,
            apiKey: String,
            allowDirectKey: Boolean = BuildConfig.DEBUG
        ): TicketmasterAccess {
            val proxy = sanitize(proxyUrl, PROXY_SENTINEL)
            if (proxy.isNotBlank()) {
                return TicketmasterAccess(Mode.PROXY, ensureTrailingSlash(proxy), "")
            }
            val key = sanitize(apiKey, KEY_SENTINEL)
            if (allowDirectKey && key.isNotBlank()) {
                return TicketmasterAccess(Mode.DIRECT, DIRECT_BASE_URL, key)
            }
            return TicketmasterAccess(Mode.OFF, "", "")
        }

        fun fromBuildConfig(
            allowDirectKey: Boolean = BuildConfig.DEBUG
        ): TicketmasterAccess = resolve(
            proxyUrl = buildConfigString("TICKETMASTER_PROXY_URL"),
            apiKey = buildConfigString("TICKETMASTER_API_KEY"),
            allowDirectKey = allowDirectKey
        )

        private fun buildConfigString(field: String): String =
            runCatching {
                BuildConfig::class.java.getDeclaredField(field).get(null) as? String
            }.getOrNull().orEmpty()

        private fun sanitize(raw: String, sentinel: String): String {
            val v = raw.trim()
            return if (v.isEmpty() || v == "null" || v == sentinel) "" else v
        }

        private fun ensureTrailingSlash(url: String): String =
            if (url.endsWith("/")) url else "$url/"
    }
}
