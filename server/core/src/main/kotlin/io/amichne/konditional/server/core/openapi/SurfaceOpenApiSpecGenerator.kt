package io.amichne.konditional.server.core.openapi

import java.io.File

internal object SurfaceOpenApiSpecGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) {
            "Expected exactly one argument: output file path."
        }
        generateTo(File(args.first()))
    }

    @JvmStatic
    fun generateTo(outputFile: File) {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(buildJson())
    }

    fun buildJson(): String = SurfaceOpenApiSpecBuilder().buildJson()
}
