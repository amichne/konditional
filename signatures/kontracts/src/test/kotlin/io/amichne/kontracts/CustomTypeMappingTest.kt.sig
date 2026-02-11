file=kontracts/src/test/kotlin/io/amichne/kontracts/CustomTypeMappingTest.kt
package=io.amichne.kontracts
imports=io.amichne.kontracts.dsl.asBoolean,io.amichne.kontracts.dsl.asDouble,io.amichne.kontracts.dsl.asInt,io.amichne.kontracts.dsl.asString,io.amichne.kontracts.dsl.of,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.StringSchema,kotlin.test.assertEquals,kotlin.test.assertIs,org.junit.jupiter.api.Test
type=io.amichne.kontracts.CustomTypeMappingTest|kind=class|decl=class CustomTypeMappingTest
type=io.amichne.kontracts.UserId|kind=class|decl=data class UserId(val value: String)
type=io.amichne.kontracts.Email|kind=class|decl=data class Email(val value: String)
type=io.amichne.kontracts.Count|kind=class|decl=data class Count(val value: Int)
type=io.amichne.kontracts.Percentage|kind=class|decl=data class Percentage(val value: Double)
type=io.amichne.kontracts.Agent|kind=class|decl=data class Agent(val isAgent: Boolean)
type=io.amichne.kontracts.UserConfig|kind=class|decl=data class UserConfig( val agent: Agent, val userId: UserId, val email: Email, val loginAttempts: Count, val completionRate: Percentage, val nickname: String, )
fields:
- val schema
methods:
- fun `custom type mapping creates correct schema types`()
- fun `custom type conversion functions are captured`()
