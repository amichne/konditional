file=konditional-serialization/src/test/kotlin/io/amichne/konditional/fixtures/serializers/PrimitiveKonstrainedFixtures.kt
package=io.amichne.konditional.fixtures.serializers
imports=io.amichne.konditional.core.types.Konstrained,io.amichne.kontracts.dsl.arraySchema,io.amichne.kontracts.dsl.booleanSchema,io.amichne.kontracts.dsl.doubleSchema,io.amichne.kontracts.dsl.elementSchema,io.amichne.kontracts.dsl.intSchema,io.amichne.kontracts.dsl.stringSchema,io.amichne.kontracts.schema.ArraySchema,io.amichne.kontracts.schema.BooleanSchema,io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.StringSchema
type=io.amichne.konditional.fixtures.serializers.Email|kind=class|decl=value class Email(val raw: String) : Konstrained<StringSchema>
type=io.amichne.konditional.fixtures.serializers.RetryCount|kind=class|decl=value class RetryCount(val value: Int) : Konstrained<IntSchema>
type=io.amichne.konditional.fixtures.serializers.FeatureEnabled|kind=class|decl=value class FeatureEnabled(val enabled: Boolean) : Konstrained<BooleanSchema>
type=io.amichne.konditional.fixtures.serializers.Percentage|kind=class|decl=value class Percentage(val value: Double) : Konstrained<DoubleSchema>
type=io.amichne.konditional.fixtures.serializers.Tags|kind=class|decl=value class Tags(val values: List<String>) : Konstrained<ArraySchema<String>>
fields:
- override val schema: StringSchema
- override val schema: IntSchema
- override val schema: BooleanSchema
- override val schema: DoubleSchema
- override val schema: ArraySchema<String>
