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

/** Value-class-backed string Konstrained with a pattern constraint. */
@JvmInline
value class Email(val raw: String) : Konstrained<StringSchema> {
    override val schema: StringSchema
        get() = stringSchema { pattern = "^[^@]+@[^@]+\\.[^@]+$" } as StringSchema
}

/** Value-class-backed int Konstrained with range constraints. */
@JvmInline
value class RetryCount(val value: Int) : Konstrained<IntSchema> {
    override val schema: IntSchema
        get() = intSchema { minimum = 0; maximum = 10 } as IntSchema
}

/** Value-class-backed boolean Konstrained. */
@JvmInline
value class FeatureEnabled(val enabled: Boolean) : Konstrained<BooleanSchema> {
    override val schema: BooleanSchema
        get() = booleanSchema { default = false } as BooleanSchema
}

/** Value-class-backed double Konstrained. */
@JvmInline
value class Percentage(val value: Double) : Konstrained<DoubleSchema> {
    override val schema: DoubleSchema
        get() = doubleSchema { minimum = 0.0; maximum = 100.0 } as DoubleSchema
}

/** Value-class-backed array Konstrained (list of non-empty strings). */
@JvmInline
value class Tags(val values: List<String>) : Konstrained<ArraySchema<String>> {
    override val schema: ArraySchema<String>
        get() = arraySchema { elementSchema(stringSchema { minLength = 1 }) } as ArraySchema<String>
}
