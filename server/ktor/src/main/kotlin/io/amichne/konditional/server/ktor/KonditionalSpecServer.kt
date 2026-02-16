package io.amichne.konditional.server.ktor

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Standalone Ktor entrypoint for exposing the Konditional REST specification.
 *
 * Boundary expectations:
 * - CLI args are optional and interpreted positionally as [port] and [path].
 * - Invalid numeric port falls back to [defaultPort].
 */
fun main(args: Array<String>) {
    val defaultPort = 8080
    val configuredPort = args.getOrNull(0)?.toIntOrNull() ?: defaultPort
    val configuredPath = args.getOrNull(1) ?: "/konditional/spec/openapi.json"

    embeddedServer(Netty, port = configuredPort) {
        installKonditionalSpecRoute(KonditionalSpecRouteConfig(path = configuredPath))
    }.start(wait = true)
}
