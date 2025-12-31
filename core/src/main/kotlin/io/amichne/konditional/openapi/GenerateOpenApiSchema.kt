package io.amichne.konditional.openapi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal object GenerateOpenApiSchema {
    private const val defaultOutputPath = "build/generated/openapi/konditional-schema.json"
    private const val defaultTitle = "Konditional Serialization Schema"

    @JvmStatic
    fun main(args: Array<String>) {
        val parsedArgs = Args.parse(args)
        val document = SerializationOpenApiDocument.document(version = parsedArgs.version, title = parsedArgs.title)
        val json = openApiJson(document)

        val primaryOutput = parsedArgs.outputPath.toAbsolutePath().normalize()
        val mirrorOutput = parsedArgs.mirrorOutputPath?.toAbsolutePath()?.normalize()

        writeJsonAtomically(primaryOutput, json)
        mirrorOutput
            ?.takeUnless { it == primaryOutput }
            ?.let { writeJsonAtomically(it, json) }

        println(
            listOfNotNull(primaryOutput, mirrorOutput?.takeUnless { it == primaryOutput })
                .joinToString(prefix = "Wrote OpenAPI schema to ", separator = "\nWrote OpenAPI schema to "),
        )
    }

    private data class Args(
        val version: String,
        val outputPath: Path,
        val title: String,
        val mirrorOutputPath: Path?,
    ) {
        companion object {
            fun parse(args: Array<String>): Args =
                Args(
                    version = args.getOrNull(0) ?: "unspecified",
                    outputPath = args.getOrNull(1)?.let(Path::of) ?: Path.of(defaultOutputPath),
                    title = args.getOrNull(2) ?: defaultTitle,
                    mirrorOutputPath = args.getOrNull(3)?.let(Path::of),
                ).also {
                    require(args.size <= 4) {
                        "Usage: GenerateOpenApiSchema [version] [outputPath] [title] [mirrorOutputPath]"
                    }
                }
        }
    }

    private fun openApiJson(document: Map<String, Any?>): String {
        val moshi =
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        return moshi.adapter<Map<String, Any?>>(mapType).indent("  ").toJson(document) + "\n"
    }

    private fun writeJsonAtomically(
        outputPath: Path,
        json: String,
    ) {
        val output = outputPath.toAbsolutePath()
        val parent = output.parent
        parent?.let(Files::createDirectories)

        val tmpPath = (parent ?: Path.of(".")).resolve("${output.fileName}.tmp")

        try {
            Files.writeString(tmpPath, json)
            moveReplacing(tmpPath, output)
        } finally {
            runCatching { Files.deleteIfExists(tmpPath) }
        }
    }

    private fun moveReplacing(
        from: Path,
        to: Path,
    ) {
        runCatching {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
