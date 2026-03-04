package io.amichne.konditional.serialization

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Fixture-backed tests for [ParseError] boundary variants.
 *
 * Each fixture is a JSON file under `src/test/resources/fixtures/parse-errors/`.
 * Tests verify that the boundary layer produces typed [ParseError] values —
 * never raw exceptions — for every failure mode.
 */
class ParseErrorFixtureTest {

    private object TestNs : Namespace.TestNamespaceFacade("parse-error-fixture-ns") {
        val flag by boolean<Context>(default = false)
    }

    private fun loadFixture(name: String): String =
        javaClass.classLoader
            .getResourceAsStream("fixtures/parse-errors/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Fixture not found: fixtures/parse-errors/$name")

    // ── invalid-json.json ────────────────────────────────────────────────────

    @Test
    fun `invalid JSON fixture produces ParseError InvalidJson — no exception thrown`() {
        val json = loadFixture("invalid-json.json")
        val result = ConfigurationCodec.decode(json = json, namespace = TestNs)

        assertTrue(result.isFailure)
        assertIs<ParseError.InvalidJson>(result.parseErrorOrNull())
    }

    // ── unknown-feature-key.json ─────────────────────────────────────────────

    @Test
    fun `unknown feature key fixture produces ParseError FeatureNotFound under strict options`() {
        val json = loadFixture("unknown-feature-key.json")
        val result = ConfigurationCodec.decode(json = json, namespace = TestNs)

        assertTrue(result.isFailure)
        assertIs<ParseError.FeatureNotFound>(result.parseErrorOrNull())
    }

    // ── null-flags.json ──────────────────────────────────────────────────────

    @Test
    fun `null flags array produces a parse failure — no exception thrown`() {
        val json = loadFixture("null-flags.json")
        val result = ConfigurationCodec.decode(json = json, namespace = TestNs)

        // Moshi refuses to deserialize null into a non-nullable List — the error
        // should surface as a typed failure, not a raw exception.
        assertTrue(result.isFailure)
        val error = result.parseErrorOrNull()
        assertTrue(error != null, "Expected a typed ParseError but got null")
    }

    // ── inline boundary variants ─────────────────────────────────────────────

    @Test
    fun `MissingRequired can be used as a typed boundary failure`() {
        val err = ParseError.missingRequired("flags[0].defaultValue")
        assertIs<ParseError.MissingRequired>(err)
        assertTrue(err.message.contains("flags[0].defaultValue"))
    }

    @Test
    fun `UnknownField can be used as a typed boundary failure`() {
        val err = ParseError.unknownField("flags[0].legacyField")
        assertIs<ParseError.UnknownField>(err)
        assertTrue(err.message.contains("flags[0].legacyField"))
    }

    @Test
    fun `InvalidValue can be used as a typed boundary failure`() {
        val err = ParseError.invalidValue("flags[0].rampUp", "must be in 0.0–100.0")
        assertIs<ParseError.InvalidValue>(err)
        assertTrue(err.path == "flags[0].rampUp")
        assertTrue(err.reason == "must be in 0.0–100.0")
    }
}
