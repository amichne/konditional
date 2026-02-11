file=kontracts/src/test/kotlin/io/amichne/kontracts/EdgeCasesAndComplexScenariosTest.kt
package=io.amichne.kontracts
imports=io.amichne.kontracts.dsl.asInt,io.amichne.kontracts.dsl.asString,io.amichne.kontracts.dsl.of,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.SchemaProvider,io.amichne.kontracts.value.JsonArray,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNull,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,io.amichne.kontracts.value.JsonValue,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.kontracts.EdgeCasesAndComplexScenariosTest|kind=class|decl=class EdgeCasesAndComplexScenariosTest
type=io.amichne.kontracts.Endpoint|kind=class|decl=data class Endpoint( val path: String, val method: String, val timeout: Int, val retries: Int, val rateLimit: Double?, val authenticated: Boolean ) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.DatabaseConfig|kind=class|decl=data class DatabaseConfig( val host: String, val port: Int, val database: String, val username: String, val maxConnections: Int, val connectionTimeout: Int, val ssl: Boolean ) : SchemaProvider<ObjectSchema>
type=io.amichne.kontracts.CustomId|kind=class|decl=data class CustomId(val value: String)
type=io.amichne.kontracts.CustomCount|kind=class|decl=data class CustomCount(val value: Int)
type=io.amichne.kontracts.Config|kind=class|decl=data class Config( val id: CustomId, val count: CustomCount, val name: String ) : SchemaProvider<ObjectSchema>
fields:
- override val schema
- override val schema
- override val schema
methods:
- fun `validates deeply nested object hierarchy`()
- fun `fails validation in deeply nested structure`()
- fun `validates array of objects with nested arrays`()
- fun `fails validation in nested array element`()
- fun `validates boundary values for numeric constraints`()
- fun `validates boundary values for string length`()
- fun `handles Double precision edge cases`()
- fun `validates strings with unicode characters`()
- fun `validates patterns with special regex characters`()
- fun `validates email pattern with complex addresses`()
- fun `validates large array of primitives`()
- fun `validates object with many fields`()
- fun `validates REST API endpoint configuration`()
- fun `validates database connection configuration`()
- fun `validates object with heterogeneous field types`()
- fun `JsonValue factory methods create correct types`()
- fun `JsonValue obj factory creates JsonObject`()
- fun `JsonValue array factory creates JsonArray`()
- fun `error messages are descriptive and actionable`()
- fun `nested error messages provide path context`()
- fun `custom types integrate seamlessly with validation`()
