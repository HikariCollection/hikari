package app.hikari.network

import okhttp3.OkHttpClient

open class NetworkHelper {
    open val client: OkHttpClient
        get() = throw UnsupportedOperationException("Stub")

    open val cookieJar: AndroidCookieJar
        get() = throw UnsupportedOperationException("Stub")

    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    open val cloudflareClient: OkHttpClient
        get() = client

    open fun defaultUserAgentProvider(): String = "Mozilla/5.0"
}
