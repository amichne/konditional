package io.amichne.konditional.openapi

internal object ConfigurationStateOpenApiDocument {
    fun document(
        version: String,
        title: String,
    ): Map<String, Any?> =
        mapOf(
            "openapi" to "3.0.3",
            "info" to mapOf("title" to title, "version" to version),
            "paths" to defaultPaths(),
            "components" to mapOf("schemas" to componentSchemas()),
        )

    private fun componentSchemas(): Map<String, Any?> =
        ConfigurationStateSchemaCatalog.schemas.mapValues { (_, schema) -> OpenApiSchemaConverter.toSchema(schema) }

    private fun defaultPaths(): Map<String, Any?> =
        mapOf(
            "/configuration-state" to mapOf(
                "get" to mapOf(
                    "summary" to "Fetch current configuration state and mutation metadata",
                    "responses" to mapOf(
                        "200" to mapOf(
                            "description" to "Configuration state payload",
                            "content" to mapOf(
                                "application/json" to mapOf(
                                    "schema" to mapOf(
                                        "\$ref" to "#/components/schemas/ConfigurationStateResponse",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
}
