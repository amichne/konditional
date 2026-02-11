file=konditional-serialization/src/test/kotlin/io/amichne/konditional/fixtures/serializers/TestSerializers.kt
package=io.amichne.konditional.fixtures.serializers
imports=io.amichne.konditional.core.types.Konstrained,io.amichne.kontracts.dsl.of,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.schema.ObjectSchema
type=io.amichne.konditional.fixtures.serializers.RetryPolicy|kind=class|decl=data class RetryPolicy( val maxAttempts: Int = 3, val backoffMs: Double = 1000.0, val enabled: Boolean = true, val mode: String = "exponential", ) : Konstrained<ObjectSchema>
type=io.amichne.konditional.fixtures.serializers.UserSettings|kind=class|decl=data class UserSettings( val theme: String = "light", val notificationsEnabled: Boolean = true, val maxRetries: Int = 3, val timeout: Double = 30.0, ) : Konstrained<ObjectSchema>
fields:
- override val schema: ObjectSchema
- override val schema
