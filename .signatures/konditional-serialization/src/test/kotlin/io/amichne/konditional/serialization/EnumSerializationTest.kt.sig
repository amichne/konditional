file=konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/EnumSerializationTest.kt
package=io.amichne.konditional.serialization
imports=com.squareup.moshi.JsonDataException,com.squareup.moshi.Moshi,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.ValueType,io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory,io.amichne.konditional.internal.serialization.models.FlagValue,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertThrows,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.EnumSerializationTest|kind=class|decl=class EnumSerializationTest
type=io.amichne.konditional.serialization.LogLevel|kind=enum|decl=enum class LogLevel
type=io.amichne.konditional.serialization.Theme|kind=enum|decl=enum class Theme
type=io.amichne.konditional.serialization.SpecialEnum|kind=enum|decl=enum class SpecialEnum
type=io.amichne.konditional.serialization.UnsupportedType|kind=class|decl=data class UnsupportedType(val value: String)
fields:
- private val moshi
- private val adapter
methods:
- fun `FlagValue from creates EnumValue for enum types`()
- fun `FlagValue from handles all enum values`()
- fun `EnumValue toValueType returns ENUM`()
- fun `EnumValue serializes to JSON correctly`()
- fun `EnumValue deserializes from JSON correctly`()
- fun `EnumValue round-trip serialization preserves value`()
- fun `EnumValue deserialization fails without value`()
- fun `EnumValue deserialization fails without enumClassName`()
- fun `EnumValue works with different enum types`()
- fun `EnumValue JSON distinguishes between different enum types`()
- fun `EnumValue handles enum with special characters in name`()
- fun `FlagValue from throws for unsupported types after adding enum support`()
- fun `EnumValue JSON format is consistent with other FlagValue types`()
