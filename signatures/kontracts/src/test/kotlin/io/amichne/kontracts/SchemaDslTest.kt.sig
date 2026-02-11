file=kontracts/src/test/kotlin/io/amichne/kontracts/SchemaDslTest.kt
package=io.amichne.kontracts
imports=io.amichne.kontracts.dsl.asBoolean,io.amichne.kontracts.dsl.asDouble,io.amichne.kontracts.dsl.asInt,io.amichne.kontracts.dsl.asString,io.amichne.kontracts.dsl.of,io.amichne.kontracts.dsl.schemaRoot,io.amichne.kontracts.schema.BooleanSchema,io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.SchemaProvider,io.amichne.kontracts.schema.StringSchema,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertIs,kotlin.test.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.kontracts.SchemaDslTest|kind=class|decl=class SchemaDslTest
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val name: String) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val count: Int) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val rate: Double) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val enabled: Boolean) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config( val name: String, val age: Int, val rate: Double, val active: Boolean ) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val nickname: String?) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val maxRetries: Int?) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val threshold: Double?) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val verified: Boolean?) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.UserId|kind=class|decl=data class UserId(val value: String)
type=io.amichne.kontracts.Email|kind=class|decl=data class Email(val value: String)
type=io.amichne.kontracts.Count|kind=class|decl=data class Count(val value: Int)
type=io.amichne.kontracts.Percentage|kind=class|decl=data class Percentage(val value: Double)
type=io.amichne.kontracts.Verified|kind=class|decl=data class Verified(val value: Boolean)
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val userId: UserId) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val loginAttempts: Count) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val completionRate: Percentage) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val verified: Verified) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val email: Email) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config( val userId: UserId, val name: String, val loginAttempts: Count, val active: Boolean ) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config( val theme: String = "light", val maxRetries: Int = 3 ) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val email: String) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val oldField: String) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val username: String) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val theme: String) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.Config|kind=class|decl=data class Config(val priority: Int) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.UserConfig|kind=class|decl=data class UserConfig( val userId: UserId, val email: Email, val displayName: String, val age: Int?, val loginAttempts: Count, val completionRate: Percentage, val isVerified: Boolean, val theme: String, val notificationsEnabled: Boolean? ) : SchemaProvider<ObjectSchema>
fields:
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
- override val schema
methods:
- fun `schemaRoot creates schema for String property`()
- fun `schemaRoot creates schema for Int property`()
- fun `schemaRoot creates schema for Double property`()
- fun `schemaRoot creates schema for Boolean property`()
- fun `schemaRoot creates schema for multiple properties`()
- fun `schemaRoot handles nullable String property`()
- fun `schemaRoot handles nullable Int property`()
- fun `schemaRoot handles nullable Double property`()
- fun `schemaRoot handles nullable Boolean property`()
- fun `asString maps custom type to StringSchema`()
- fun `asInt maps custom type to IntSchema`()
- fun `asDouble maps custom type to DoubleSchema`()
- fun `asBoolean maps custom type to BooleanSchema`()
- fun `custom type mapping preserves all constraints`()
- fun `schemaRoot handles mix of standard and custom types`()
- fun `schemaRoot captures default values`()
- fun `schemaRoot captures examples`()
- fun `schemaRoot captures deprecation status`()
- fun `schemaRoot captures title`()
- fun `schemaRoot handles string enum constraints`()
- fun `schemaRoot handles int enum constraints`()
- fun `schemaRoot creates empty schema when no properties defined`()
- fun `schemaRoot handles complex configuration with all features`()
