package io.amichne.konditional.demo.tobebuilt

internal data class ToBeBuiltRenderState(
    val inputsApiTimeoutMs: Int = InputsDefaults.apiTimeoutMs,
    val schemaForms: SchemaFormsModel = SchemaFormsModel.EMPTY,
)

internal object InputsDefaults {
    const val apiTimeoutMs: Int = 5000
    const val apiTimeoutMinMs: Int = 1000
    const val apiTimeoutMaxMs: Int = 10000
    const val apiTimeoutStepMs: Int = 1000
}

internal data class SchemaFormsModel(
    val flagsByNamespace: Map<String, List<SchemaFlagSummary>>,
) {
    companion object {
        val EMPTY: SchemaFormsModel = SchemaFormsModel(flagsByNamespace = emptyMap())
    }
}

internal data class SchemaFlagSummary(
    val key: String,
    val encodedKey: String,
    val namespace: String,
    val shortKey: String,
    val type: String,
    val rulesCount: Int,
    val isActive: Boolean,
)

