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
    override val value: kotlin.Boolean get() = enabled
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
        get() = arraySchema { elementSchema(stringSchema { minLength = 1 }) } as ArraySchema<String>
}

// ---------------------------------------------------------------------------
// As* family — domain type T encoded AS a JSON primitive
// ---------------------------------------------------------------------------

/**
 * A calendar date serialized as an ISO-8601 string (e.g. `"2025-06-15"`).
 *
 * Demonstrates [Konstrained.AsString] with a non-primitive domain type ([LocalDate]).
 * No explicit schema override — the default unconstrained [StringSchema] is used.
 * The companion [Konstrained.StringDecoder] reconstructs the value from the wire string.
 */
@JvmInline
value class ExpirationDate(val value: LocalDate) : Konstrained.AsString<LocalDate> {
    override fun encode(): String = value.toString()

    companion object : Konstrained.StringDecoder<ExpirationDate> {
        override fun decode(raw: String): ExpirationDate = ExpirationDate(LocalDate.parse(raw))
    }
}

/**
 * A calendar date with an explicit schema override declaring the `"date"` OpenAPI format.
 *
 * Demonstrates optional schema customization on top of [Konstrained.AsString].
 */
@JvmInline
value class AuditDate(val value: LocalDate) : Konstrained.AsString<LocalDate> {
    @Suppress("UNCHECKED_CAST")
    override val schema: StringSchema
        get() = stringSchema { format = "date" } as StringSchema

    override fun encode(): String = value.toString()

    companion object : Konstrained.StringDecoder<AuditDate> {
        override fun decode(raw: String): AuditDate = AuditDate(LocalDate.parse(raw))
    }
}

/**
 * A [UUID] correlation identifier serialized as its canonical hyphenated string form.
 *
 * Demonstrates [Konstrained.AsString] with a second non-primitive domain type.
 */
@JvmInline
value class CorrelationId(val value: UUID) : Konstrained.AsString<UUID> {
    override fun encode(): String = value.toString()

    companion object : Konstrained.StringDecoder<CorrelationId> {
        override fun decode(raw: String): CorrelationId = CorrelationId(UUID.fromString(raw))
    }
}
