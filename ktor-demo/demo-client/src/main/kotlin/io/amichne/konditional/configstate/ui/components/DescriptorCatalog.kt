package io.amichne.konditional.configstate.ui.components

import io.amichne.konditional.configstate.ui.model.FieldDescriptorDto
import io.amichne.konditional.configstate.ui.model.FieldTypeDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mui.material.Paper
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.useMemo
import react.useState
import web.cssom.px

external interface DescriptorCatalogProps : Props {
    var byType: Map<String, FieldDescriptorDto>
}

val DescriptorCatalog: FC<DescriptorCatalogProps> =
    FC { props ->
        val initialValues =
            useMemo(props.byType) {
                props.byType.mapValues { (fieldType, descriptor) -> sampleValue(fieldType, descriptor) }
            }

        val (values, setValues) = useState<Map<String, JsonElement>>(initialValues)

        Stack {
            Typography { +"Field Descriptor Catalog" }

            props.byType.entries.sortedBy { it.key }.forEach { (fieldTypeRaw, descriptor) ->
                val fieldType = runCatching { FieldTypeDto.valueOf(fieldTypeRaw) }.getOrNull()
                    ?: return@forEach

                val current = values[fieldTypeRaw] ?: sampleValue(fieldTypeRaw, descriptor)

                Paper {
                    sx { padding = 16.px }

                    Stack {
                        Typography {
                            +fieldTypeRaw
                        }

                        FieldEditor {
                            this.fieldType = fieldType
                            this.descriptor = descriptor
                            this.value = current
                            onChange = { next ->
                                setValues(values.toMutableMap().apply { put(fieldTypeRaw, next) })
                            }
                        }
                    }
                }
            }
        }
    }

private fun sampleValue(
    fieldTypeRaw: String,
    descriptor: FieldDescriptorDto,
): JsonElement =
    when (runCatching { FieldTypeDto.valueOf(fieldTypeRaw) }.getOrNull()) {
        FieldTypeDto.FLAG_ACTIVE -> JsonPrimitive(true)
        FieldTypeDto.SALT -> JsonPrimitive("v1")
        FieldTypeDto.RAMP_UP_PERCENT -> JsonPrimitive(42.0)
        FieldTypeDto.RAMP_UP_ALLOWLIST -> JsonArray(listOf(JsonPrimitive("deadbeefcafebabe1234567890abcdef")))
        FieldTypeDto.LOCALES -> JsonArray(listOf(JsonPrimitive("en_US")))
        FieldTypeDto.PLATFORMS -> JsonArray(listOf(JsonPrimitive("web")))
        FieldTypeDto.RULE_NOTE -> JsonPrimitive("Human-readable note for this rule.")
        FieldTypeDto.AXES_MAP ->
            JsonObject(
                mapOf(
                    "environment" to JsonArray(listOf(JsonPrimitive("prod"), JsonPrimitive("staging"))),
                    "tier" to JsonArray(listOf(JsonPrimitive("free"))),
                ),
            )
        FieldTypeDto.FLAG_VALUE ->
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("BOOLEAN"),
                    "value" to JsonPrimitive(true),
                ),
            )
        FieldTypeDto.VERSION_RANGE ->
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("MIN_AND_MAX_BOUND"),
                    "min" to JsonPrimitive("1.0.0"),
                    "max" to JsonPrimitive("2.0.0"),
                ),
            )
        FieldTypeDto.SEMVER -> JsonPrimitive("1.2.3")
        else -> sampleFromUiHints(descriptor)
    }

private fun sampleFromUiHints(descriptor: FieldDescriptorDto): JsonElement =
    when (descriptor.uiHints.control) {
        else -> JsonPrimitive("")
    }
