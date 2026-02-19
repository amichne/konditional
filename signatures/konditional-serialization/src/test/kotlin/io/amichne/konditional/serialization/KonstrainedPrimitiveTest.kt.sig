file=konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedPrimitiveTest.kt
package=io.amichne.konditional.serialization
imports=com.squareup.moshi.Moshi,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.fixtures.core.withOverride,io.amichne.konditional.fixtures.serializers.Email,io.amichne.konditional.fixtures.serializers.FeatureEnabled,io.amichne.konditional.fixtures.serializers.Percentage,io.amichne.konditional.fixtures.serializers.RetryCount,io.amichne.konditional.fixtures.serializers.Tags,io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory,io.amichne.konditional.internal.serialization.models.FlagValue,io.amichne.konditional.serialization.instance.ConfigValue,io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonString,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertInstanceOf,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.KonstrainedPrimitiveTest|kind=class|decl=class KonstrainedPrimitiveTest
fields:
- private val moshi
- private val adapter
- private val context
methods:
- fun `encodeKonstrained produces JsonString for StringSchema-backed value class`()
- fun `encodeKonstrained produces JsonNumber for IntSchema-backed value class`()
- fun `encodeKonstrained produces JsonBoolean for BooleanSchema-backed value class`()
- fun `encodeKonstrained produces JsonNumber for DoubleSchema-backed value class`()
- fun `encodeKonstrained produces JsonArray for ArraySchema-backed value class`()
- fun `decodeKonstrainedPrimitive wraps String back into Email`()
- fun `decodeKonstrainedPrimitive wraps Int back into RetryCount`()
- fun `decodeKonstrainedPrimitive wraps Boolean back into FeatureEnabled`()
- fun `decodeKonstrainedPrimitive wraps Double back into Percentage`()
- fun `decodeKonstrainedPrimitive wraps List back into Tags`()
- fun `FlagValue from Email produces KonstrainedPrimitive`()
- fun `FlagValue from RetryCount produces KonstrainedPrimitive`()
- fun `FlagValue from Tags produces KonstrainedPrimitive with list value`()
- fun `KonstrainedPrimitive extractValue round-trips Email`()
- fun `KonstrainedPrimitive extractValue round-trips RetryCount`()
- fun `KonstrainedPrimitive extractValue round-trips FeatureEnabled`()
- fun `KonstrainedPrimitive extractValue round-trips Percentage`()
- fun `KonstrainedPrimitive extractValue round-trips Tags`()
- fun `KonstrainedPrimitive Moshi round-trip for String value`()
- fun `KonstrainedPrimitive Moshi round-trip for Int value`()
- fun `KonstrainedPrimitive Moshi round-trip for Boolean value`()
- fun `KonstrainedPrimitive Moshi round-trip for List value`()
- fun `KonstrainedPrimitive full end-to-end Moshi round-trip reconstructs value class`()
- fun `ConfigValue from Email produces KonstrainedPrimitive`()
- fun `ConfigValue from Tags produces KonstrainedPrimitive with list`()
- fun `feature flag with Email Konstrained evaluates default and override correctly`()
- fun `feature flag with RetryCount evaluates default and override correctly`()
- fun `feature flag with primitive Konstrained serializes and deserializes via ConfigurationSnapshotCodec`()
- fun `feature flag with Tags Konstrained evaluates default and override correctly`()
