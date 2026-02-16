package io.amichne.konditional.server.ktor.spec

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Immutable settings for running a standalone Ktor server for the REST specification route.
 */
public data class KonditionalRestSpecServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val routeConfig: KonditionalRestSpecRouteConfig = KonditionalRestSpecRouteConfig(),
)

/**
 * Launches a standalone Ktor Netty server exposing the configured REST specification route.
 */
public fun runKonditionalRestSpecServer(config: KonditionalRestSpecServerConfig = KonditionalRestSpecServerConfig()) {
    embeddedServer(Netty, host = config.host, port = config.port) {
        installKonditionalRestSpec(config.routeConfig)
    }.start(wait = true)
}

/**
 * CLI entrypoint for running the REST specification server independently.
 *
 * Args:
 * - `--host=<host>` defaults to `0.0.0.0`
 * - `--port=<port>` defaults to `8080`
 * - `--path=<route-path>` defaults to `/openapi/konditional-rest-spec.json`
 */
public object KonditionalRestSpecStandaloneServer {
    @JvmStatic
    public fun main(args: Array<String>) {
        val config = parseServerArgs(args)
        runKonditionalRestSpecServer(config)
    }
}

internal fun parseServerArgs(args: Array<String>): KonditionalRestSpecServerConfig {
    val values =
        args
            .mapNotNull { token ->
                token.split("=", limit = 2).takeIf { it.size == 2 }
            }.associate { (key, value) -> key to value }

    val parsedPort = values["--port"]?.toIntOrNull() ?: 8080

    return KonditionalRestSpecServerConfig(
        host = values["--host"] ?: "0.0.0.0",
        port = parsedPort,
        routeConfig = KonditionalRestSpecRouteConfig(routePath = values["--path"] ?: "/openapi/konditional-rest-spec.json"),
    )
}
