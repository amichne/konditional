package io.amichne.kontracts

import io.amichne.kontracts.dsl.enumSchema
import io.amichne.kontracts.dsl.stringSchema
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonStringAllowedValuesTest {
    private enum class Environment { DEV, PROD }

    @Test
    fun `string schema enforces its allowed strings`() {
        val schema = stringSchema { enum = listOf("DEV") }

        assertTrue(JsonString("DEV").validate(schema).isValid)
        assertFalse(JsonString("PROD").validate(schema).isValid)
    }

    @Test
    fun `enum schema enforces its allowed subset of enum constants`() {
        val schema = enumSchema(Environment::class) { values = listOf(Environment.DEV) }

        assertTrue(JsonString("DEV").validate(schema).isValid)
        assertFalse(JsonString("PROD").validate(schema).isValid)
    }
}
