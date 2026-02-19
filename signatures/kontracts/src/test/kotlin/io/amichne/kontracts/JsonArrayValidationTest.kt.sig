file=kontracts/src/test/kotlin/io/amichne/kontracts/JsonArrayValidationTest.kt
package=io.amichne.kontracts
imports=io.amichne.kontracts.dsl.arraySchema,io.amichne.kontracts.dsl.booleanSchema,io.amichne.kontracts.dsl.elementSchema,io.amichne.kontracts.dsl.fieldSchema,io.amichne.kontracts.dsl.intSchema,io.amichne.kontracts.dsl.objectSchema,io.amichne.kontracts.dsl.stringSchema,io.amichne.kontracts.value.JsonArray,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertNull,kotlin.test.assertTrue,org.junit.jupiter.api.Test,org.junit.jupiter.api.assertThrows
type=io.amichne.kontracts.JsonArrayValidationTest|kind=class|decl=class JsonArrayValidationTest
methods:
- fun `JsonArray validates successfully with homogeneous elements`()
- fun `JsonArray validates empty array`()
- fun `JsonArray fails validation with heterogeneous elements`()
- fun `JsonArray validates array of integers`()
- fun `JsonArray validates array of booleans`()
- fun `JsonArray fails validation when element violates schema constraints`()
- fun `JsonArray validates array of objects successfully`()
- fun `JsonArray fails validation when object element is invalid`()
- fun `JsonArray validates nested arrays successfully`()
- fun `JsonArray fails validation with invalid nested array element`()
- fun `JsonArray get operator retrieves element by index`()
- fun `JsonArray size returns correct count`()
- fun `JsonArray isEmpty returns true for empty array`()
- fun `JsonArray isEmpty returns false for non-empty array`()
- fun `JsonArray fails validation against non-array schema`()
- fun `JsonArray construction validates elements against provided schema`()
- fun `JsonArray construction succeeds with valid elements`()
- fun `JsonArray validates with complex element constraints`()
- fun `JsonArray fails validation when element exceeds constraints`()
- fun `JsonArray validates array with objects containing nested arrays`()
- fun `JsonArray toString formats correctly`()
- fun `JsonArray empty array toString`()
