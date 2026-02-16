package io.amichne.konditional.httpserver

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.nio.file.Path

private const val NAMESPACE_PREFIX = "/v1/namespaces/"

public fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val storagePath = Path.of(System.getenv("STORAGE_PATH") ?: "/data/snapshots.json")
    val storage = FileBackedSnapshotStorage(storagePath)

    println("konditional-http-server started on port $port, storage=$storagePath")
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
    ) {
        konditionalHttpServer(storage)
    }.start(wait = true)
}

internal fun Application.konditionalHttpServer(
    storage: FileBackedSnapshotStorage,
) {
    routing {
        route("{...}") {
            handle {
                val requestPath = call.request.path()
                val requestMethod = call.request.httpMethod

                when {
                    requestPath == "/health" -> {
                        call.respondText(
                            text = "ok",
                            status = HttpStatusCode.OK,
                        )
                    }

                    requestPath == "/v1/namespaces" && requestMethod == HttpMethod.Get -> {
                        val namespaces = storage.all().keys
                            .joinToString(prefix = "[", separator = ",", postfix = "]") { key -> "\"$key\"" }
                        call.respondText(
                            text = namespaces,
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK,
                        )
                    }

                    requestPath.startsWith(NAMESPACE_PREFIX) -> {
                        val namespace = requestPath.removePrefix(NAMESPACE_PREFIX)
                        if (namespace.isBlank()) {
                            call.respondText(
                                text = "namespace must not be blank",
                                status = HttpStatusCode.BadRequest,
                            )
                            return@handle
                        }
                        call.handleNamespaceCall(storage, namespace)
                    }

                    else -> call.respondText(
                        text = "not found",
                        status = HttpStatusCode.NotFound,
                    )
                }
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.handleNamespaceCall(
    storage: FileBackedSnapshotStorage,
    namespace: String,
) {
    when (request.httpMethod) {
        HttpMethod.Get -> {
            val payload = storage.get(namespace)
            if (payload == null) {
                respondText(
                    text = "namespace not found",
                    status = HttpStatusCode.NotFound,
                )
            } else {
                respondText(
                    text = payload,
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                )
            }
        }

        HttpMethod.Put -> {
            val requestBody = receiveText()
            if (requestBody.isBlank()) {
                respondText(
                    text = "request body must not be blank",
                    status = HttpStatusCode.BadRequest,
                )
                return
            }
            storage.put(namespace, requestBody)
            respond(HttpStatusCode.NoContent)
        }

        HttpMethod.Delete -> {
            if (storage.delete(namespace)) {
                respond(HttpStatusCode.NoContent)
            } else {
                respondText(
                    text = "namespace not found",
                    status = HttpStatusCode.NotFound,
                )
            }
        }

        else -> respondText(
            text = "method not allowed",
            status = HttpStatusCode.MethodNotAllowed,
        )
    }
}
