package io.amichne.konditional.types

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.result.toParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LeafTypesTest {
    private enum class TestEnvironment(
        val id: String,
    ) {
        DEV("dev"),
        PROD("prod"),
    }

    @Test
    fun contextFactorySuppliesOnlyDeclaredFacts() {
        val deployment = object : FactSchema() {
            val environment = single("environment", FactDomain.enumeration(TestEnvironment.entries) { it.id })
        }
        val context = Context(Facts.of(deployment.environment.value(TestEnvironment.PROD)))

        assertTrue(deployment.environment.oneOf(TestEnvironment.PROD).matches(context))
    }

    @Test
    fun parseResultPreservesTypedBoundaryFailures() {
        val result = parseFailure<Unit>(ParseError.invalidJson("bad payload")).toParseResult()

        val failure = assertIs<ParseResult.Failure>(result)
        assertEquals(ParseError.invalidJson("bad payload"), failure.error)
    }
}
