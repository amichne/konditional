package io.amichne.konditional.server.core

import io.amichne.konditional.server.core.openapi.SurfaceOpenApiSpecGenerator

/**
 * Public access point for the deterministic Konditional REST surface specification.
 *
 * Invariants:
 * - The output is generated from stable route and schema catalogs.
 * - Repeated invocations with identical catalogs return byte-identical JSON.
 */
public object SurfaceRestSpecification {
    /**
     * Builds the OpenAPI JSON document for the Konditional REST surface.
     */
    public fun openApiJson(): String = SurfaceOpenApiSpecGenerator.buildJson()
}
