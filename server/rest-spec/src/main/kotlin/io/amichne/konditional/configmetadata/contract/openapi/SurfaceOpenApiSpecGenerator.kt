package io.amichne.konditional.configmetadata.contract.openapi

import java.io.File

internal object SurfaceOpenApiSpecGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) {
            "Expected exactly one argument: output file path."
        }
        RestSurfaceOpenApiSpec.writeTo(File(args.first()))
    }

}
