@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.withOverride
import io.amichne.konditional.fixtures.serializers.AuditDate
import io.amichne.konditional.fixtures.serializers.CorrelationId
import io.amichne.konditional.fixtures.serializers.ExpirationDate
import io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.serialization.instance.ConfigValue
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Tests for [io.amichne.konditional.core.types.Konstrained.AsString] (and family) support.
 *
 * Validates:
 * - [SchemaValueCodec.encodeKonstrained] uses the instance [encode] method
 * - [SchemaValueCodec.decodeKonstrainedPrimitive] uses the companion [Konstrained.Decoder]
 * - The instance [decode] method is the inverse of [encode] (round-trip via interface)
 * - [FlagValue.from] / [FlagValue.extractValue] round-trip correctness
 * - Moshi serialization / deserialization of [FlagValue.KonstrainedPrimitive]
 * - [ConfigValue.from] dispatches correctly for As* types
 * - Feature flag integration with custom-type Konstrained
 * - Schema default vs explicit override behaviour
 */
class KonstrainedCustomTypeTest {

    private val moshi = Moshi.Builder()
        .add(FlagValueAdapterFactory)
        .build()

    private val adapter = moshi.adapter(FlagValue::class.java)

    private val context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version(1, 0, 0),
        stableId = TestStableId,
    )

    // =========================================================================
    // SchemaValueCodec.encodeKonstrained — As* dispatch
    // =========================================================================

    @Test
    fun `encodeKonstrained produces JsonString for AsString-backed LocalDate`() {
        val date = ExpirationDate(LocalDate.of(2025, 6, 15))
        val encoded = SchemaValueCodec.encodeKonstrained(date)
        assertInstanceOf(JsonString::class.java, encoded)
        assertEquals("2025-06-15", (encoded as JsonString).value)
    }

    @Test
    fun `encodeKonstrained produces JsonString for AsString-backed UUID`() {
        val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val correlationId = CorrelationId(id)
        val encoded = SchemaValueCodec.encodeKonstrained(correlationId)
        assertInstanceOf(JsonString::class.java, encoded)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", (encoded as JsonString).value)
    }

    @Test
    fun `encodeKonstrained uses encode() method not property extraction for AsString`() {
        // ExpirationDate.value is LocalDate, NOT String.
        // If the codec incorrectly tried extractSinglePrimitiveProperty<String>() it would
        // throw. Reaching this assertion proves the encode() dispatch path was taken.
        val date = ExpirationDate(LocalDate.of(2024, 1, 1))
        val encoded = SchemaValueCodec.encodeKonstrained(date)
        assertInstanceOf(JsonString::class.java, encoded)
    }

    // =========================================================================
    // SchemaValueCodec.decodeKonstrainedPrimitive — companion Konstrained.Decoder
    // =========================================================================

    @Test
    fun `decodeKonstrainedPrimitive uses companion Decoder for ExpirationDate`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(ExpirationDate::class, "2025-06-15")
        assertTrue(result.isSuccess)
        assertEquals(ExpirationDate(LocalDate.of(2025, 6, 15)), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive uses companion Decoder for AuditDate`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(AuditDate::class, "2023-11-30")
        assertTrue(result.isSuccess)
        assertEquals(AuditDate(LocalDate.of(2023, 11, 30)), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive uses companion Decoder for CorrelationId`() {
        val raw = "550e8400-e29b-41d4-a716-446655440000"
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(CorrelationId::class, raw)
        assertTrue(result.isSuccess)
        assertEquals(CorrelationId(UUID.fromString(raw)), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive returns failure when Decoder throws`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(ExpirationDate::class, "not-a-date")
        assertTrue(result.isFailure)
    }

    // =========================================================================
    // Instance decode() — declared on AsString interface, inverse of encode()
    // =========================================================================

    @Test
    fun `instance decode is the inverse of encode for ExpirationDate`() {
        val original = ExpirationDate(LocalDate.of(2025, 6, 15))
        val roundTripped = original.decode(original.encode())
        assertEquals(original, roundTripped)
    }

    @Test
    fun `instance decode is the inverse of encode for AuditDate`() {
        val original = AuditDate(LocalDate.of(2023, 11, 30))
        val roundTripped = original.decode(original.encode())
        assertEquals(original, roundTripped)
    }

    @Test
    fun `instance decode is the inverse of encode for CorrelationId`() {
        val original = CorrelationId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val roundTripped = original.decode(original.encode())
        assertEquals(original, roundTripped)
    }

    @Test
    fun `ExpirationDate and AuditDate share the same underlying localDateDecoder logic`() {
        // Both types wrap LocalDate and parse via the same shared Decoder — verify they
        // produce structurally equal domain values from the same wire string.
        val raw = "2025-06-15"
        val expiry = ExpirationDate(LocalDate.of(2025, 6, 15))
        val audit = AuditDate(LocalDate.of(2025, 6, 15))
        assertEquals(expiry.value, audit.decode(raw).value)
        assertEquals(expiry.decode(raw).value, audit.value)
    }

    // =========================================================================
    // FlagValue round-trip
    // =========================================================================

    @Test
    fun `FlagValue from ExpirationDate produces KonstrainedPrimitive with string value`() {
        val date = ExpirationDate(LocalDate.of(2025, 3, 1))
        val fv = FlagValue.from(date)
        assertInstanceOf(FlagValue.KonstrainedPrimitive::class.java, fv)
        fv as FlagValue.KonstrainedPrimitive
        assertEquals("2025-03-01", fv.value)
        assertEquals(ExpirationDate::class.java.name, fv.konstrainedClassName)
    }

    @Test
    fun `FlagValue extractValue round-trips ExpirationDate`() {
        val original = ExpirationDate(LocalDate.of(2026, 12, 31))
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<ExpirationDate>())
    }

    @Test
    fun `FlagValue extractValue round-trips CorrelationId`() {
        val original = CorrelationId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<CorrelationId>())
    }

    @Test
    fun `FlagValue extractValue round-trips AuditDate with schema override`() {
        val original = AuditDate(LocalDate.of(2024, 7, 4))
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<AuditDate>())
    }

    // =========================================================================
    // Moshi serialization round-trip
    // =========================================================================

    @Test
    fun `KonstrainedPrimitive Moshi round-trip for ExpirationDate`() {
        val original = FlagValue.KonstrainedPrimitive(
            value = "2025-06-15",
            konstrainedClassName = ExpirationDate::class.java.name,
        )
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json) as FlagValue.KonstrainedPrimitive
        assertEquals("2025-06-15", deserialized.value)
        assertEquals(ExpirationDate::class.java.name, deserialized.konstrainedClassName)
    }

    @Test
    fun `KonstrainedPrimitive full end-to-end Moshi round-trip reconstructs ExpirationDate`() {
        val original = ExpirationDate(LocalDate.of(2025, 6, 15))
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        val json = adapter.toJson(fv)
        val deserialized = adapter.fromJson(json) as FlagValue.KonstrainedPrimitive
        assertEquals(original, deserialized.extractValue<ExpirationDate>())
    }

    // =========================================================================
    // Schema behaviour
    // =========================================================================

    @Test
    fun `ExpirationDate uses default StringSchema when no override provided`() {
        val date = ExpirationDate(LocalDate.of(2025, 1, 1))
        val schema = date.schema
        // Default schema has no constraints
        assertEquals(null, schema.format)
        assertEquals(null, schema.pattern)
    }

    @Test
    fun `AuditDate uses overridden StringSchema with format date`() {
        val date = AuditDate(LocalDate.of(2025, 1, 1))
        val schema = date.schema
        assertEquals("date", schema.format)
    }

    // =========================================================================
    // ConfigValue dispatch
    // =========================================================================

    @Test
    fun `ConfigValue from ExpirationDate produces KonstrainedPrimitive`() {
        val cv = ConfigValue.from(ExpirationDate(LocalDate.of(2025, 1, 1)))
        assertInstanceOf(ConfigValue.KonstrainedPrimitive::class.java, cv)
        cv as ConfigValue.KonstrainedPrimitive
        assertEquals("2025-01-01", cv.rawValue)
        assertEquals(ExpirationDate::class.java.name, cv.konstrainedClassName)
    }

    // =========================================================================
    // Feature flag integration
    // =========================================================================

    @Test
    fun `feature flag with ExpirationDate evaluates default and override correctly`() {
        val defaultDate = ExpirationDate(LocalDate.of(2025, 1, 1))
        val overrideDate = ExpirationDate(LocalDate.of(2026, 6, 30))

        val features = object : Namespace.TestNamespaceFacade("expiry-flag") {
            val expiry by custom<ExpirationDate, Context>(default = defaultDate) {}
        }

        assertEquals(defaultDate, features.expiry.evaluate(context))

        features.withOverride(features.expiry, overrideDate) {
            assertEquals(overrideDate, features.expiry.evaluate(context))
        }
    }

    @Test
    fun `feature flag with CorrelationId evaluates default and override correctly`() {
        val defaultId = CorrelationId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val overrideId = CorrelationId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))

        val features = object : Namespace.TestNamespaceFacade("correlation-flag") {
            val correlationId by custom<CorrelationId, Context>(default = defaultId) {}
        }

        assertEquals(defaultId, features.correlationId.evaluate(context))

        features.withOverride(features.correlationId, overrideId) {
            assertEquals(overrideId, features.correlationId.evaluate(context))
        }
    }

    @Test
    fun `feature flag with ExpirationDate serializes and deserializes via snapshot codec`() {
        val defaultDate = ExpirationDate(LocalDate.of(2025, 1, 1))
        val overrideDate = ExpirationDate(LocalDate.of(2026, 6, 30))

        val features = object : Namespace.TestNamespaceFacade("snapshot-expiry-flag") {
            val expiry by custom<ExpirationDate, Context>(default = defaultDate) {}
        }

        features.withOverride(features.expiry, overrideDate) {
            val json = ConfigurationCodec.encode(features.configuration)
            val decoded = ConfigurationCodec.decode(
                json = json,
                schema = features.compiledSchema(),
            )
            assertTrue(
                decoded.isSuccess,
                "Snapshot decode should succeed: ${decoded.exceptionOrNull()?.message}",
            )
        }
    }
}
