package com.example.data.remote

import com.example.BuildConfig

/**
 * How the app reaches Ticketmaster Discovery.
 *
 * Release builds should use [Mode.PROXY] (key lives in the Cloudflare Worker).
 * Debug may fall back to a local key in BuildConfig ([Mode.DIRECT]).
 * When proxying, [clientToken] is sent as `X-DeadSet-Client` if non-blank (F4).
 */
data class TicketmasterAccess(
    val mode: Mode,
    /** OkHttp base URL ending with `/`, empty when [Mode.OFF]. */
    val baseUrl: String,
    /** Empty when using the proxy (Worker injects the secret). */
    val apiKey: String,
    /**
     * Shared with Worker secret `DEADSET_CLIENT_TOKEN`. Empty = do not send the header
     * (Worker must also leave that secret unset).
     */
    val clientToken: String = ""
) {
    enum class Mode { PROXY, DIRECT, OFF }

    val isConfigured: Boolean get() = mode != Mode.OFF

    /** Headers for Ticketmaster HTTP calls; empty when not using the proxy or token blank. */
    fun requestHeaders(): Map<String, String> {
        if (mode != Mode.PROXY || clientToken.isBlank()) return emptyMap()
        return mapOf(CLIENT_HEADER to clientToken)
    }

    companion object {
        const val DIRECT_BASE_URL = "https://app.ticketmaster.com/"
        const val CLIENT_HEADER = "X-DeadSet-Client"
        private const val PROXY_SENTINEL = "YOUR_TICKETMASTER_PROXY_URL"
        private const val KEY_SENTINEL = "YOUR_TICKETMASTER_API_KEY"
        private const val CLIENT_SENTINEL = "YOUR_DEADSET_CLIENT_TOKEN"

        fun resolve(
            proxyUrl: String,
            apiKey: String,
            clientToken: String = "",
            allowDirectKey: Boolean = BuildConfig.DEBUG
        ): TicketmasterAccess {
            val proxy = sanitize(proxyUrl, PROXY_SENTINEL)
            val token = sanitize(clientToken, CLIENT_SENTINEL)
            if (proxy.isNotBlank()) {
                return TicketmasterAccess(Mode.PROXY, ensureTrailingSlash(proxy), "", token)
            }
            val key = sanitize(apiKey, KEY_SENTINEL)
            if (allowDirectKey && key.isNotBlank()) {
                return TicketmasterAccess(Mode.DIRECT, DIRECT_BASE_URL, key, "")
            }
            return TicketmasterAccess(Mode.OFF, "", "", "")
        }

        fun fromBuildConfig(
            allowDirectKey: Boolean = BuildConfig.DEBUG
        ): TicketmasterAccess = resolve(
            proxyUrl = buildConfigString("TICKETMASTER_PROXY_URL"),
            apiKey = buildConfigString("TICKETMASTER_API_KEY"),
            clientToken = buildConfigString("DEADSET_CLIENT_TOKEN"),
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
