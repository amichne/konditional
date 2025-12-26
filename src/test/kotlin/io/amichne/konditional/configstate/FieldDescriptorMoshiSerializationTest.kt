package io.amichne.konditional.configstate

import io.amichne.konditional.serialization.SnapshotSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldDescriptorMoshiSerializationTest {
    private val moshi = SnapshotSerializer.defaultMoshi()
    private val adapter = moshi.adapter(FieldDescriptor::class.java).indent("  ")

    @Test
    fun `serialized FieldDescriptor includes kind discriminator`() {
        val hints = UiHints(control = UiControlType.TEXT, label = "Label")

        val samples: List<Pair<FieldDescriptor.Kind, FieldDescriptor>> =
            listOf(
                FieldDescriptor.Kind.BOOLEAN to BooleanDescriptor(uiHints = hints),
                FieldDescriptor.Kind.ENUM_OPTIONS to EnumOptionsDescriptor(uiHints = hints, options = listOf(Option("a", "A"))),
                FieldDescriptor.Kind.NUMBER_RANGE to NumberRangeDescriptor(uiHints = hints, min = 0.0, max = 10.0, step = 1.0),
                FieldDescriptor.Kind.SEMVER_CONSTRAINTS to SemverConstraintsDescriptor(uiHints = hints, minimum = "1.0.0"),
                FieldDescriptor.Kind.STRING_CONSTRAINTS to StringConstraintsDescriptor(uiHints = hints, minLength = 1),
                FieldDescriptor.Kind.SCHEMA_REF to SchemaRefDescriptor(uiHints = hints.copy(control = UiControlType.JSON), ref = "FlagValue"),
                FieldDescriptor.Kind.MAP_CONSTRAINTS to
                    MapConstraintsDescriptor(
                        uiHints = hints.copy(control = UiControlType.KEY_VALUE),
                        key = StringConstraintsDescriptor(uiHints = hints, minLength = 1),
                        values = StringConstraintsDescriptor(uiHints = hints, minLength = 1),
                    ),
            )

        val kindsInJson =
            samples.associate { (kind, descriptor) ->
                kind to adapter.toJson(descriptor)
            }

        kindsInJson.forEach { (expectedKind, json) ->
            assertTrue(
                json.contains(""""kind": "${expectedKind.name}""""),
                "Expected kind discriminator for $expectedKind. JSON was:\n$json",
            )
        }
    }

    @Test
    fun `FieldDescriptor kind must be consistent on decode`() {
        val hints = UiHints(control = UiControlType.JSON, label = "Value")
        val original = SchemaRefDescriptor(uiHints = hints, ref = "FlagValue")

        val json = adapter.toJson(original)
        val decoded = adapter.fromJson(json)

        requireNotNull(decoded)
        assertEquals(FieldDescriptor.Kind.SCHEMA_REF, decoded.kind)
    }
}

