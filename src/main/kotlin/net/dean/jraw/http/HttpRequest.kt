package net.dean.jraw.http

import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Models a HTTP request. Create instances using the Builder model and are executed asynchronously by default.
 *
 * ```
 * val request = HttpRequest.Builder()
 *     .method("DELETE") // defaults to GET
 *     .url("https://httpbin.org/delete")
 *     .success({ println(it.json) })
 *     .failure({ println("Failed: $it" })
 *     .build()
 *
 * val response = httpClient.execute(request)
 * println(response.json)
 * ```
 *
 * Note that [HttpAdapter.executeSync] will ignore [success] and [failure]. You'll have to handle possible errors via
 * try/catch.
 */
class HttpRequest private constructor(
    val url: String,
    val headers: Headers.Builder,
    val method: String,
    val body: RequestBody?,
    internal val basicAuth: BasicAuthData?,
    internal val success: (response: HttpResponse) -> Unit,
    internal val failure: (response: HttpResponse) -> Unit,
    internal val internalFailure: (original: Request, e: IOException) -> Unit
) {
    private constructor(b: Builder) : this(
        url = buildUrl(b),
        headers = b.headers,
        method = b.method,
        body = b.body,
        basicAuth = b.basicAuth,
        success = b.success,
        failure = b.failure,
        internalFailure = b.internalFailure
    )

    companion object {
        internal val DEFAULT_FAILURE_HANDLER: (response: HttpResponse) -> Unit = { res ->
            val req = res.raw.request()
            throw RuntimeException("Unhandled HTTP request with non-success status: ${req.method()} ${req.url()} -> ${res.code}")
        }
        internal val DEFAULT_INTERNAL_FAILURE_HANDLER: (original: Request, e: IOException) -> Unit = { r, e ->
            throw createInternalFailureException(r, e)
        }
        internal fun createInternalFailureException(r: Request, e: IOException): RuntimeException =
            RuntimeException("HTTP request engine encountered an error: ${r.method()} ${r.url()}", e)

        /** This Pattern will match a URI parameter. For example, /api/{param1}/{param2}  */
        private val PATH_PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}")

        private fun buildUrl(b: Builder): String {
            if (b.url != null) return b.url!!

            if (b.host.trim() == "") throw IllegalArgumentException("Expecting a non-empty host")
            var path = b.path.trim()
            if (!path.startsWith("/")) path = "/" + path
            if (!b.pathParams.isEmpty()) path = substitutePathParameters(b.path, b.pathParams)

            return "http${if (b.secure) "s" else ""}://${b.host}$path${buildQuery(b.query)}"
        }

        private fun buildQuery(q: Map<String, String>): String {
            if (q.isEmpty()) return ""
            val sb = StringBuilder("?")
            with (sb) {
                val it = q.entries.iterator()
                while (it.hasNext()) {
                    val (k, v) = it.next()
                    append(URLEncoder.encode(k, "UTF-8"))
                    append('=')
                    append(URLEncoder.encode(v, "UTF-8"))
                }
            }

            return sb.toString()
        }

        private fun substitutePathParameters(path: String, positionalArgs: List<String>): String {
            val pathParams = parsePathParams(path)
            if (pathParams.size != positionalArgs.size) {
                // Different amount of parameters
                throw IllegalArgumentException(
                    "URL parameter size mismatch. Expecting ${pathParams.size}, got ${positionalArgs.size}")
            }

            var updatedUri = path
            var m: Matcher? = null
            for (arg in positionalArgs) {
                if (m == null) {
                    // Create on first use
                    m = PATH_PARAM_PATTERN.matcher(updatedUri)
                } else {
                    // Reuse the Matcher
                    m.reset(updatedUri)
                }
                updatedUri = m!!.replaceFirst(arg)
            }

            return updatedUri
        }

        /** Finds all parameters in the given path  */
        private fun parsePathParams(path: String): List<String> {
            val params = ArrayList<String>()
            val matcher = PATH_PARAM_PATTERN.matcher(path)
            while (matcher.find()) {
                params.add(matcher.group())
            }

            return params
        }
    }

    class Builder {
        internal var method: String = "GET"
        internal var headers: Headers.Builder = Headers.Builder()
        internal var body: RequestBody? = null
        internal var success: (response: HttpResponse) -> Unit = {}
        internal var failure: (response: HttpResponse) -> Unit = DEFAULT_FAILURE_HANDLER
        internal var internalFailure: (original: Request, e: IOException) -> Unit = DEFAULT_INTERNAL_FAILURE_HANDLER

        // URL-related variables
        internal var url: String? = null
        internal var secure = true
        internal var host = ""
        internal var path = ""
        internal var pathParams = listOf<String>()
        internal var query: Map<String, String> = mapOf()
        internal var basicAuth: BasicAuthData? = null

        /** Sets the HTTP method (GET, POST, PUT, etc.). Defaults to GET. Case insensitive. */
        fun method(method: String, body: RequestBody? = null): Builder {
            this.method = method.trim().toUpperCase()
            this.body = body
            return this
        }
        fun method(method: String, body: Map<String, String>): Builder {
            this.method = method
            val formBodyBuilder = FormBody.Builder()
            for ((k, v) in body)
                formBodyBuilder.addEncoded(k, v)
            this.body = formBodyBuilder.build()
            return this
        }

        fun get() = method("GET")
        fun delete() = method("DELETE")
        fun post(body: Map<String, String>) = method("POST", body)
        fun post(body: RequestBody) = method("POST", body)
        fun put(body: Map<String, String>) = method("PUT", body)
        fun put(body: RequestBody) = method("PUT", body)
        fun patch(body: Map<String, String>) = method("PATCH", body)
        fun patch(body: RequestBody) = method("PATCH", body)

        fun url(url: String): Builder { this.url = url; return this }

        fun addHeader(key: String, value: String): Builder { this.headers.add(key, value); return this }

        /** Sets the function that gets called on a 2XX status code */
        fun success(success: (response: HttpResponse) -> Unit): Builder { this.success = success; return this }

        /** Sets the function that gets called on a non-2XX status code  */
        fun failure(failure: (response: HttpResponse) -> Unit): Builder { this.failure = failure; return this }

        /** Sets the callback to handle an internal HTTP engine failure */
        fun internalFailure(internalFailure: (original: Request, e: IOException) -> Unit): Builder {
            this.internalFailure = internalFailure
            return this
        }

        /** Convenience function for `basicAuth(BasicAuthData)` */
        fun basicAuth(creds: Pair<String, String>) = basicAuth(BasicAuthData(creds.first, creds.second))

        /** Executes the request with HTTP basic authentication */
        fun basicAuth(creds: BasicAuthData): Builder { this.basicAuth = creds; return this }

        /** Enables/disables HTTPS (enabled by default) */
        fun secure(flag: Boolean = true): Builder { this.secure = flag; return this }
        /** Sets the hostname (e.g. "google.com" or "oauth.reddit.com") */
        fun host(host: String): Builder { this.host = host; return this }

        /**
         * Sets the URL's path. For example, "/thatJavaNerd/JRAW." Positional path parameters are supported, so if
         * {@code path} was "/api/{resource}" and {@code params} was a one-element array consisting of "foo", then the
         * resulting path would be "/api/foo."
         *
         * @param path The path. If null, "/" will be used.
         * @param pathParams Optional positional path parameters
         * @return This Builder
         */
        fun path(path: String, vararg pathParams: String): Builder {
            this.path = path
            this.pathParams = listOf(*pathParams)
            return this
        }

        fun query(query: Map<String, String>): Builder { this.query = query; return this }

        fun build() = HttpRequest(this)
    }
}