package io.amichne.konditional.server.ktor

/**
 * Typed configuration for where the spec endpoint is exposed.
 *
 * Determinism assumptions:
 * - [path] must be stable for the server process lifetime.
 */
data class KonditionalSpecRouteConfig(
    val path: String = "/konditional/spec/openapi.json",
)
