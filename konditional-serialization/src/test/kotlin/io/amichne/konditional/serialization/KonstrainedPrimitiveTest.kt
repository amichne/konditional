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
import io.amichne.konditional.fixtures.serializers.Email
import io.amichne.konditional.fixtures.serializers.FeatureEnabled
import io.amichne.konditional.fixtures.serializers.Percentage
import io.amichne.konditional.fixtures.serializers.RetryCount
import io.amichne.konditional.fixtures.serializers.Tags
import io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.serialization.instance.ConfigValue
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for primitive/array-backed [io.amichne.konditional.core.types.Konstrained] support.
 *
 * Validates:
 * - [SchemaValueCodec.encodeKonstrained] for all primitive schema types
 * - [SchemaValueCodec.decodeKonstrainedPrimitive] round-trip correctness
 * - [FlagValue.from] / [FlagValue.extractValue] round-trip for [FlagValue.KonstrainedPrimitive]
 * - Moshi serialization / deserialization of [FlagValue.KonstrainedPrimitive]
 * - [ConfigValue.from] dispatches correctly for primitive-backed types
 * - Feature flag integration with primitive-backed Konstrained
 */
class KonstrainedPrimitiveTest {

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

    // ========== SchemaValueCodec.encodeKonstrained ==========

    @Test
    fun `encodeKonstrained produces JsonString for StringSchema-backed value class`() {
        val email = Email("user@example.com")
        val encoded = SchemaValueCodec.encodeKonstrained(email)
        assertInstanceOf(JsonString::class.java, encoded)
        assertEquals("user@example.com", (encoded as JsonString).value)
    }

    @Test
    fun `encodeKonstrained produces JsonNumber for IntSchema-backed value class`() {
        val count = RetryCount(5)
        val encoded = SchemaValueCodec.encodeKonstrained(count)
        assertInstanceOf(JsonNumber::class.java, encoded)
        assertEquals(5, (encoded as JsonNumber).toInt())
    }

    @Test
    fun `encodeKonstrained produces JsonBoolean for BooleanSchema-backed value class`() {
        val flag = FeatureEnabled(true)
        val encoded = SchemaValueCodec.encodeKonstrained(flag)
        assertInstanceOf(JsonBoolean::class.java, encoded)
        assertEquals(true, (encoded as JsonBoolean).value)
    }

    @Test
    fun `encodeKonstrained produces JsonNumber for DoubleSchema-backed value class`() {
        val pct = Percentage(42.5)
        val encoded = SchemaValueCodec.encodeKonstrained(pct)
        assertInstanceOf(JsonNumber::class.java, encoded)
        assertEquals(42.5, (encoded as JsonNumber).toDouble(), 0.0001)
    }

    @Test
    fun `encodeKonstrained produces JsonArray for ArraySchema-backed value class`() {
        val tags = Tags(listOf("kotlin", "feature-flags"))
        val encoded = SchemaValueCodec.encodeKonstrained(tags)
        assertInstanceOf(io.amichne.kontracts.value.JsonArray::class.java, encoded)
        val array = encoded as io.amichne.kontracts.value.JsonArray
        assertEquals(2, array.elements.size)
        assertEquals("kotlin", (array.elements[0] as JsonString).value)
        assertEquals("feature-flags", (array.elements[1] as JsonString).value)
    }

    // ========== SchemaValueCodec.decodeKonstrainedPrimitive ==========

    @Test
    fun `decodeKonstrainedPrimitive wraps String back into Email`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(Email::class, "hello@world.com")
        assertTrue(result.isSuccess)
        assertEquals(Email("hello@world.com"), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive wraps Int back into RetryCount`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(RetryCount::class, 3)
        assertTrue(result.isSuccess)
        assertEquals(RetryCount(3), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive wraps Boolean back into FeatureEnabled`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(FeatureEnabled::class, false)
        assertTrue(result.isSuccess)
        assertEquals(FeatureEnabled(false), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive wraps Double back into Percentage`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(Percentage::class, 99.9)
        assertTrue(result.isSuccess)
        assertEquals(Percentage(99.9), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrainedPrimitive wraps List back into Tags`() {
        val result = SchemaValueCodec.decodeKonstrainedPrimitive(Tags::class, listOf("a", "b"))
        assertTrue(result.isSuccess)
        assertEquals(Tags(listOf("a", "b")), result.getOrThrow())
    }

    // ========== FlagValue.from dispatch ==========

    @Test
    fun `FlagValue from Email produces KonstrainedPrimitive`() {
        val fv = FlagValue.from(Email("a@b.com"))
        assertInstanceOf(FlagValue.KonstrainedPrimitive::class.java, fv)
        fv as FlagValue.KonstrainedPrimitive
        assertEquals("a@b.com", fv.value)
        assertEquals(Email::class.java.name, fv.konstrainedClassName)
    }

    @Test
    fun `FlagValue from RetryCount produces KonstrainedPrimitive`() {
        val fv = FlagValue.from(RetryCount(7))
        assertInstanceOf(FlagValue.KonstrainedPrimitive::class.java, fv)
        fv as FlagValue.KonstrainedPrimitive
        assertEquals(7, fv.value)
    }

    @Test
    fun `FlagValue from Tags produces KonstrainedPrimitive with list value`() {
        val fv = FlagValue.from(Tags(listOf("x", "y")))
        assertInstanceOf(FlagValue.KonstrainedPrimitive::class.java, fv)
        fv as FlagValue.KonstrainedPrimitive
        assertEquals(listOf("x", "y"), fv.value)
    }

    // ========== FlagValue.extractValue round-trip ==========

    @Test
    fun `KonstrainedPrimitive extractValue round-trips Email`() {
        val original = Email("round@trip.io")
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        val decoded = fv.extractValue<Email>()
        assertEquals(original, decoded)
    }

    @Test
    fun `KonstrainedPrimitive extractValue round-trips RetryCount`() {
        val original = RetryCount(2)
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<RetryCount>())
    }

    @Test
    fun `KonstrainedPrimitive extractValue round-trips FeatureEnabled`() {
        val original = FeatureEnabled(true)
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<FeatureEnabled>())
    }

    @Test
    fun `KonstrainedPrimitive extractValue round-trips Percentage`() {
        val original = Percentage(55.0)
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<Percentage>())
    }

    @Test
    fun `KonstrainedPrimitive extractValue round-trips Tags`() {
        val original = Tags(listOf("alpha", "beta"))
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        assertEquals(original, fv.extractValue<Tags>())
    }

    // ========== Moshi serialization round-trip ==========

    @Test
    fun `KonstrainedPrimitive Moshi round-trip for String value`() {
        val original = FlagValue.KonstrainedPrimitive(
            value = "hello@world.com",
            konstrainedClassName = Email::class.java.name,
        )
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)
        assertInstanceOf(FlagValue.KonstrainedPrimitive::class.java, deserialized)
        deserialized as FlagValue.KonstrainedPrimitive
        assertEquals("hello@world.com", deserialized.value)
        assertEquals(Email::class.java.name, deserialized.konstrainedClassName)
    }

    @Test
    fun `KonstrainedPrimitive Moshi round-trip for Int value`() {
        val original = FlagValue.KonstrainedPrimitive(
            value = 5,
            konstrainedClassName = RetryCount::class.java.name,
        )
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json) as FlagValue.KonstrainedPrimitive
        assertEquals(5, deserialized.value)
        assertEquals(RetryCount::class.java.name, deserialized.konstrainedClassName)
    }

    @Test
    fun `KonstrainedPrimitive Moshi round-trip for Boolean value`() {
        val original = FlagValue.KonstrainedPrimitive(
            value = true,
            konstrainedClassName = FeatureEnabled::class.java.name,
        )
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json) as FlagValue.KonstrainedPrimitive
        assertEquals(true, deserialized.value)
    }

    @Test
    fun `KonstrainedPrimitive Moshi round-trip for List value`() {
        val original = FlagValue.KonstrainedPrimitive(
            value = listOf("x", "y"),
            konstrainedClassName = Tags::class.java.name,
        )
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json) as FlagValue.KonstrainedPrimitive
        assertEquals(listOf("x", "y"), deserialized.value)
    }

    @Test
    fun `KonstrainedPrimitive full end-to-end Moshi round-trip reconstructs value class`() {
        val original = Email("end@to-end.com")
        val fv = FlagValue.from(original) as FlagValue.KonstrainedPrimitive
        val json = adapter.toJson(fv)
        val deserialized = adapter.fromJson(json) as FlagValue.KonstrainedPrimitive
        assertEquals(original, deserialized.extractValue<Email>())
    }

    // ========== ConfigValue dispatch ==========

    @Test
    fun `ConfigValue from Email produces KonstrainedPrimitive`() {
        val cv = ConfigValue.from(Email("cfg@value.com"))
        assertInstanceOf(ConfigValue.KonstrainedPrimitive::class.java, cv)
        cv as ConfigValue.KonstrainedPrimitive
        assertEquals("cfg@value.com", cv.rawValue)
        assertEquals(Email::class.java.name, cv.konstrainedClassName)
    }

    @Test
    fun `ConfigValue from Tags produces KonstrainedPrimitive with list`() {
        val cv = ConfigValue.from(Tags(listOf("t1", "t2")))
        assertInstanceOf(ConfigValue.KonstrainedPrimitive::class.java, cv)
        cv as ConfigValue.KonstrainedPrimitive
        assertEquals(listOf("t1", "t2"), cv.rawValue)
    }

    // ========== Feature flag integration ==========

    @Test
    fun `feature flag with Email Konstrained evaluates default and override correctly`() {
        val defaultEmail = Email("default@domain.com")
        val overrideEmail = Email("override@domain.com")

        val features = object : Namespace.TestNamespaceFacade("email-flag") {
            val recipientEmail by custom<Email, Context>(default = defaultEmail) {}
        }

        assertEquals(defaultEmail, features.recipientEmail.evaluate(context))

        features.withOverride(features.recipientEmail, overrideEmail) {
            assertEquals(overrideEmail, features.recipientEmail.evaluate(context))
        }
    }

    @Test
    fun `feature flag with RetryCount evaluates default and override correctly`() {
        val defaultCount = RetryCount(3)
        val overrideCount = RetryCount(10)

        val features = object : Namespace.TestNamespaceFacade("retry-flag") {
            val retries by custom<RetryCount, Context>(default = defaultCount) {}
        }

        assertEquals(defaultCount, features.retries.evaluate(context))

        features.withOverride(features.retries, overrideCount) {
            assertEquals(overrideCount, features.retries.evaluate(context))
        }
    }

    @Test
    fun `feature flag with primitive Konstrained serializes and deserializes via ConfigurationSnapshotCodec`() {
        val defaultEmail = Email("snapshot@domain.com")
        val overrideEmail = Email("snap-override@domain.com")

        val features = object : Namespace.TestNamespaceFacade("snapshot-email-flag") {
            val email by custom<Email, Context>(default = defaultEmail) {}
        }

        features.withOverride(features.email, overrideEmail) {
            val json = ConfigurationSnapshotCodec.encode(features.configuration)
            val decoded = ConfigurationSnapshotCodec.decode(
                json = json,
                schema = features.compiledSchema(),
            )
            assertTrue(decoded.isSuccess, "Snapshot decode should succeed: ${decoded.exceptionOrNull()?.message}")
        }
    }

    @Test
    fun `feature flag with Tags Konstrained evaluates default and override correctly`() {
        val defaultTags = Tags(listOf("default"))
        val overrideTags = Tags(listOf("override", "extra"))

        val features = object : Namespace.TestNamespaceFacade("tags-flag") {
            val tags by custom<Tags, Context>(default = defaultTags) {}
        }

        assertEquals(defaultTags, features.tags.evaluate(context))

        features.withOverride(features.tags, overrideTags) {
            assertEquals(overrideTags, features.tags.evaluate(context))
        }
    }
}
