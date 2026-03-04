package io.amichne.konditional.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * [SnapshotFetcher] backed by [java.net.http.HttpClient].
 *
 * Issues a single GET to [url] on each call. Returns [Result.failure] if the
 * response status is not 2xx or if a network error occurs. Never throws.
 *
 * Intended for use with the Konditional HTTP server (`GET /v1/namespaces/{ns}`).
 *
 * @param url full URL of the snapshot endpoint, e.g. `http://localhost:8080/v1/namespaces/app`
 * @param client shared [HttpClient]; defaults to a new instance with JVM defaults
 */
class HttpSnapshotFetcher(
    private val url: String,
    private val client: HttpClient = HttpClient.newHttpClient(),
) : SnapshotFetcher {
    override suspend fun fetch(): Result<String> = runCatching {
        val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        check(response.statusCode() in HTTP_SUCCESS_RANGE) {
            "HTTP ${response.statusCode()} fetching snapshot from $url"
        }
        response.body()
    }

    private companion object {
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
