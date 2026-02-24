package io.amichne.konditional.fixtures.serializers

import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.arraySchema
import io.amichne.kontracts.dsl.booleanSchema
import io.amichne.kontracts.dsl.doubleSchema
import io.amichne.kontracts.dsl.elementSchema
import io.amichne.kontracts.dsl.intSchema
import io.amichne.kontracts.dsl.stringSchema
import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.StringSchema
import java.time.LocalDate
import java.util.UUID

// ---------------------------------------------------------------------------
// Primitive family — value IS the JSON primitive
// ---------------------------------------------------------------------------

/** Value-class-backed string Konstrained with a pattern constraint. */
@JvmInline
value class Email(override val value: String) : Konstrained.Primitive.String<StringSchema> {
    override val schema: StringSchema
        get() = stringSchema { pattern = "^[^@]+@[^@]+\\.[^@]+$" } as StringSchema
}

/** Value-class-backed int Konstrained with range constraints. */
@JvmInline
value class RetryCount(override val value: Int) : Konstrained.Primitive.Int<IntSchema> {
    override val schema: IntSchema
        get() = intSchema { minimum = 0; maximum = 10 } as IntSchema
}

/** Value-class-backed boolean Konstrained. */
@JvmInline
value class FeatureEnabled(val enabled: Boolean) : Konstrained.Primitive.Boolean<BooleanSchema> {
    override val value: Boolean get() = enabled
    override val schema: BooleanSchema
        get() = booleanSchema { default = false } as BooleanSchema
}

/** Value-class-backed double Konstrained. */
@JvmInline
value class Percentage(override val value: Double) : Konstrained.Primitive.Double<DoubleSchema> {
    override val schema: DoubleSchema
        get() = doubleSchema { minimum = 0.0; maximum = 100.0 } as DoubleSchema
}

/** Value-class-backed array Konstrained (list of non-empty strings). */
@JvmInline
value class Tags(override val values: List<String>) : Konstrained.Array<ArraySchema<String>, String> {
    override val schema: ArraySchema<String>
        get() = arraySchema { elementSchema(stringSchema { minLength = 1 }) }
}

// ---------------------------------------------------------------------------
// As* family — domain type T encoded AS a JSON primitive
// ---------------------------------------------------------------------------

// Shared Decoder instances — reused across multiple types that wrap the same domain type,
// eliminating duplicate parse/format logic. These are the Konstrained.Decoder analogue
// of a Moshi TypeAdapter that can be applied to many fields.
private val localDateDecoder: Konstrained.Decoder<String, LocalDate> =
    Konstrained.Decoder { LocalDate.parse(it) }

private val uuidDecoder: Konstrained.Decoder<String, UUID> =
    Konstrained.Decoder { UUID.fromString(it) }

/**
 * A calendar date serialized as an ISO-8601 string (e.g. `"2025-06-15"`).
 *
 * Demonstrates [Konstrained.AsString] with a non-primitive domain type ([LocalDate]).
 * No explicit schema override — the default unconstrained [StringSchema] is used.
 *
 * Both [encode] and [decode] are declared on the [Konstrained.AsString] interface,
 * making the full codec contract visible without any hidden companion conventions.
 * The companion implements [Konstrained.Decoder] so the serialization codec can
 * reconstruct instances from snapshots (where no prototype instance is available).
 * The instance [decode] delegates to the companion to keep logic in one place.
 */
@JvmInline
value class ExpirationDate(override val value: LocalDate) : Konstrained.AsString<LocalDate, ExpirationDate> {
    override fun encode(): String = value.toString()
    override fun decode(raw: String): ExpirationDate = Companion.decode(raw)

    companion object : Konstrained.Decoder<String, ExpirationDate> {
        override fun decode(raw: String): ExpirationDate = ExpirationDate(localDateDecoder.decode(raw))
    }
}

/**
 * A calendar date with an explicit schema override declaring the `"date"` OpenAPI format.
 *
 * Demonstrates optional schema customization on top of [Konstrained.AsString].
 * Reuses [localDateDecoder] — the same [Konstrained.Decoder] shared with [ExpirationDate].
 */
@JvmInline
value class AuditDate(override val value: LocalDate) : Konstrained.AsString<LocalDate, AuditDate> {
    @Suppress("UNCHECKED_CAST")
    override val schema: StringSchema
        get() = stringSchema { format = "date" } as StringSchema

    override fun encode(): String = value.toString()
    override fun decode(raw: String): AuditDate = Companion.decode(raw)

    companion object : Konstrained.Decoder<String, AuditDate> {
        override fun decode(raw: String): AuditDate = AuditDate(localDateDecoder.decode(raw))
    }
}

/**
 * A [UUID] correlation identifier serialized as its canonical hyphenated string form.
 *
 * Demonstrates [Konstrained.AsString] with a second non-primitive domain type,
 * reusing [uuidDecoder] for the parse step.
 */
@JvmInline
value class CorrelationId(override val value: UUID) : Konstrained.AsString<UUID, CorrelationId> {
    override fun encode(): String = value.toString()
    override fun decode(raw: String): CorrelationId = Companion.decode(raw)

    companion object : Konstrained.Decoder<String, CorrelationId> {
        override fun decode(raw: String): CorrelationId = CorrelationId(uuidDecoder.decode(raw))
    }
}
