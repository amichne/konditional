package io.amichne.konditional.httpserver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path

private const val NAMESPACE_PREFIX = "/v1/namespaces/"

public fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val storagePath = Path.of(System.getenv("STORAGE_PATH") ?: "/data/snapshots.json")
    val storage = FileBackedSnapshotStorage(storagePath)

    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/health") { exchange ->
        exchange.respond(200, "ok")
    }
    server.createContext("/v1/namespaces", NamespaceHandler(storage))
    server.executor = null
    server.start()

    println("konditional-http-server started on port $port, storage=$storagePath")
}

private class NamespaceHandler(
    private val storage: FileBackedSnapshotStorage,
) : HttpHandler {
    override fun handle(exchange: HttpExchange) {
        when {
            exchange.requestURI.path == "/v1/namespaces" && exchange.requestMethod == "GET" -> {
                val namespaces = storage.all().keys
                    .joinToString(prefix = "[", separator = ",", postfix = "]") { key -> "\"$key\"" }
                exchange.respond(200, namespaces, contentType = "application/json")
            }

            exchange.requestURI.path.startsWith(NAMESPACE_PREFIX) -> {
                val namespace = exchange.requestURI.path.removePrefix(NAMESPACE_PREFIX)
                if (namespace.isBlank()) {
                    exchange.respond(400, "namespace must not be blank")
                    return
                }
                handleNamespace(exchange, namespace)
            }

            else -> exchange.respond(404, "not found")
        }
    }

    private fun handleNamespace(exchange: HttpExchange, namespace: String) {
        when (exchange.requestMethod) {
            "GET" -> {
                val payload = storage.get(namespace)
                if (payload == null) {
                    exchange.respond(404, "namespace not found")
                } else {
                    exchange.respond(200, payload, contentType = "application/json")
                }
            }

            "PUT" -> {
                val requestBody = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                if (requestBody.isBlank()) {
                    exchange.respond(400, "request body must not be blank")
                    return
                }
                storage.put(namespace, requestBody)
                exchange.respond(204, "")
            }

            "DELETE" -> {
                if (storage.delete(namespace)) {
                    exchange.respond(204, "")
                } else {
                    exchange.respond(404, "namespace not found")
                }
            }

            else -> exchange.respond(405, "method not allowed")
        }
    }
}

private fun HttpExchange.respond(
    status: Int,
    body: String,
    contentType: String = "text/plain; charset=utf-8",
) {
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    responseHeaders.add("Content-Type", contentType)
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}
