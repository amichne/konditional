file=kontracts/src/test/kotlin/io/amichne/kontracts/JsonObjectValidationTest.kt
package=io.amichne.kontracts
imports=io.amichne.kontracts.dsl.booleanSchema,io.amichne.kontracts.dsl.fieldSchema,io.amichne.kontracts.dsl.intSchema,io.amichne.kontracts.dsl.objectSchema,io.amichne.kontracts.dsl.stringSchema,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNull,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertNull,kotlin.test.assertTrue,org.junit.jupiter.api.Test,org.junit.jupiter.api.assertThrows
type=io.amichne.kontracts.JsonObjectValidationTest|kind=class|decl=class JsonObjectValidationTest
methods:
- fun `JsonObject validates successfully with all required fields`()
- fun `JsonObject fails validation with missing required field`()
- fun `JsonObject fails validation with unknown field`()
- fun `JsonObject validates with optional fields present`()
- fun `JsonObject validates with optional fields absent`()
- fun `JsonObject fails validation when field has wrong type`()
- fun `JsonObject validation throws on construction with schema mismatch`()
- fun `JsonObject validates nested objects successfully`()
- fun `JsonObject fails validation with invalid nested object`()
- fun `JsonObject validates deeply nested objects`()
- fun `JsonObject get operator retrieves field by name`()
- fun `JsonObject getTyped returns correctly typed values`()
- fun `JsonObject getTyped returns null for type mismatch`()
- fun `JsonObject getTyped handles nested objects`()
- fun `JsonObject validates empty object with no required fields`()
- fun `JsonObject fails validation for empty object with required fields`()
- fun `JsonObject fails validation against non-object schema`()
- fun `JsonObject validates with mixed required and optional fields`()
- fun `JsonObject toString formats correctly`()
