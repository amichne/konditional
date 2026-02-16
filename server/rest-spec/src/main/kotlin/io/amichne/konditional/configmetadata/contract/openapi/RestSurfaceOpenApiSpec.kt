package io.amichne.konditional.configmetadata.contract.openapi

import java.io.File

/**
 * Public contract surface for rendering Konditional's REST OpenAPI specification.
 *
 * Invariants:
 * - Rendering is deterministic for identical route/schema inputs.
 * - Output bytes are stable across invocations within the same artifact version.
 * - No runtime side effects beyond writing requested target files.
 */
public object RestSurfaceOpenApiSpec {
    /**
     * Builds the REST OpenAPI specification JSON payload.
     */
    public fun renderJson(): String = SurfaceOpenApiSpecBuilder().buildJson()

    /**
     * Writes the rendered REST OpenAPI specification to [outputFile].
     */
    public fun writeTo(outputFile: File) {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(renderJson())
    }
}
