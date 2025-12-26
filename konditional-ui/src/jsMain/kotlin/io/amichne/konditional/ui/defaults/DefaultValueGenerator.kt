package io.amichne.konditional.ui.defaults

import io.amichne.konditional.ui.model.ArrayDescriptorDto
import io.amichne.konditional.ui.model.BooleanDescriptorDto
import io.amichne.konditional.ui.model.EnumOptionsDescriptorDto
import io.amichne.konditional.ui.model.FieldDescriptorDto
import io.amichne.konditional.ui.model.MapConstraintsDescriptorDto
import io.amichne.konditional.ui.model.NumberRangeDescriptorDto
import io.amichne.konditional.ui.model.ObjectDescriptorDto
import io.amichne.konditional.ui.model.SchemaRefDescriptorDto
import io.amichne.konditional.ui.model.SemverConstraintsDescriptorDto
import io.amichne.konditional.ui.model.StringConstraintsDescriptorDto
import io.amichne.konditional.ui.model.UiControlTypeDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Generates sensible default values from field descriptors.
 *
 * Used when adding new array items or initializing object properties.
 */
object DefaultValueGenerator {
    /**
     * Generates a default value for the given descriptor.
     */
    fun generate(descriptor: FieldDescriptorDto): JsonElement =
        when (descriptor) {
            is BooleanDescriptorDto -> JsonPrimitive(false)

            is NumberRangeDescriptorDto -> JsonPrimitive(descriptor.min)

            is EnumOptionsDescriptorDto -> generateEnum(descriptor)

            is StringConstraintsDescriptorDto -> generateString(descriptor)

            is SemverConstraintsDescriptorDto -> JsonPrimitive(descriptor.minimum)

            is MapConstraintsDescriptorDto -> JsonObject(emptyMap())

            is SchemaRefDescriptorDto -> generateFromSchema(descriptor)

            is ArrayDescriptorDto -> generateArray(descriptor)

            is ObjectDescriptorDto -> generateObject(descriptor)
        }

    private fun generateEnum(descriptor: EnumOptionsDescriptorDto): JsonElement =
        when (descriptor.uiHints.control) {
            UiControlTypeDto.MULTISELECT -> JsonArray(emptyList())
            else -> descriptor.options.firstOrNull()?.value?.let(::JsonPrimitive)
                ?: JsonPrimitive("")
        }

    private fun generateString(descriptor: StringConstraintsDescriptorDto): JsonElement {
        // Use first suggestion if available
        descriptor.suggestions?.firstOrNull()?.let {
            return JsonPrimitive(it)
        }

        // Use placeholder if available
        descriptor.uiHints.placeholder?.let {
            return JsonPrimitive(it)
        }

        // Generate minimal valid string
        val minLength = descriptor.minLength ?: 0
        return when {
            minLength > 0 -> JsonPrimitive("a".repeat(minLength))
            else -> JsonPrimitive("")
        }
    }

    private fun generateFromSchema(descriptor: SchemaRefDescriptorDto): JsonElement =
        when {
            descriptor.ref.contains("object", ignoreCase = true) -> JsonObject(emptyMap())
            descriptor.ref.contains("array", ignoreCase = true) -> JsonArray(emptyList())
            else -> JsonObject(emptyMap())
        }

    private fun generateArray(descriptor: ArrayDescriptorDto): JsonElement {
        val minItems = descriptor.minItems ?: 0
        return if (minItems > 0) {
            val items = (0 until minItems).map { generate(descriptor.itemDescriptor) }
            JsonArray(items)
        } else {
            JsonArray(emptyList())
        }
    }

    private fun generateObject(descriptor: ObjectDescriptorDto): JsonElement {
        val properties = mutableMapOf<String, JsonElement>()

        // Generate required properties
        descriptor.required.forEach { key ->
            descriptor.properties[key]?.let { propDesc ->
                properties[key] = generate(propDesc.descriptor)
            }
        }

        // Optionally generate all properties for better UX
        descriptor.properties.forEach { (key, propDesc) ->
            if (key !in properties) {
                properties[key] = generate(propDesc.descriptor)
            }
        }

        return JsonObject(properties)
    }

    /**
     * Generates a sample value for display/preview purposes.
     *
     * May return more interesting values than [generate] for demo purposes.
     */
    fun sample(descriptor: FieldDescriptorDto): JsonElement =
        when (descriptor) {
            is BooleanDescriptorDto -> JsonPrimitive(true)

            is NumberRangeDescriptorDto -> {
                // Pick midpoint for more interesting sample
                val mid = (descriptor.min + descriptor.max) / 2
                JsonPrimitive(mid)
            }

            is EnumOptionsDescriptorDto -> {
                // Pick second option if available for variety
                val option = descriptor.options.getOrNull(1) ?: descriptor.options.firstOrNull()
                when (descriptor.uiHints.control) {
                    UiControlTypeDto.MULTISELECT -> {
                        val values = descriptor.options.take(2).map { JsonPrimitive(it.value) }
                        JsonArray(values)
                    }
                    else -> option?.value?.let(::JsonPrimitive) ?: JsonPrimitive("")
                }
            }

            is StringConstraintsDescriptorDto -> {
                descriptor.suggestions?.firstOrNull()?.let(::JsonPrimitive)
                    ?: JsonPrimitive("example")
            }

            is SemverConstraintsDescriptorDto -> JsonPrimitive(descriptor.minimum)

            is MapConstraintsDescriptorDto -> {
                JsonObject(
                    mapOf(
                        "example_key" to JsonArray(listOf(JsonPrimitive("value1"), JsonPrimitive("value2"))),
                    ),
                )
            }

            is SchemaRefDescriptorDto -> JsonObject(
                mapOf(
                    "property" to JsonPrimitive("value"),
                ),
            )

            is ArrayDescriptorDto -> {
                val sampleItems = (0 until 2).map { sample(descriptor.itemDescriptor) }
                JsonArray(sampleItems)
            }

            is ObjectDescriptorDto -> {
                val properties = descriptor.properties.entries
                    .sortedBy { it.value.order }
                    .associate { (key, propDesc) -> key to sample(propDesc.descriptor) }
                JsonObject(properties)
            }
        }
}
