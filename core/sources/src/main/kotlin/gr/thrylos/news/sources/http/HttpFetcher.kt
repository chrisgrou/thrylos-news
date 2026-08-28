package gr.thrylos.news.sources.http

import gr.thrylos.news.sources.plugin.HttpConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/128.0 Mobile Safari/537.36 ThrylosNews/1.0"

/** In-memory only, per [HttpFetcher] instance — enough for a cookie a site sets on
 *  one request (e.g. a consent/session cookie) to be sent back on the next one
 *  during the same sync/test run. Never persisted, never shared across instances. */
private class InMemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = store[url.host].orEmpty()
}

class HttpFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .cookieJar(InMemoryCookieJar())
        .build(),
) {
    /** Fetches [url] as text, honoring the plugin's [HttpConfig] (user agent + extra headers). */
    fun fetchText(url: String, http: HttpConfig = HttpConfig()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", if (http.userAgent == "default") DEFAULT_USER_AGENT else http.userAgent)
            .header("Accept-Language", "el-GR,el;q=0.9,en;q=0.5")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

        http.headers.forEach { (key, value) -> requestBuilder.header(key, value) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} για $url" }
            return response.body?.string() ?: error("Κενό σώμα απάντησης για $url")
        }
    }
}
