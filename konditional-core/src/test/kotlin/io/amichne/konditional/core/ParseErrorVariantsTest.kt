package io.amichne.konditional.core

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.result.parseErrorOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ParseErrorVariantsTest {

    // ── UnknownField ─────────────────────────────────────────────────────────

    @Test
    fun `UnknownField message includes path`() {
        val err = ParseError.UnknownField(path = "flags[0].extraField")
        assertTrue(err.message.contains("flags[0].extraField"))
    }

    @Test
    fun `UnknownField accepts custom message`() {
        val err = ParseError.UnknownField(path = "flags[0].x", message = "custom msg")
        assertEquals("custom msg", err.message)
    }

    @Test
    fun `UnknownField data class equality`() {
        assertEquals(
            ParseError.UnknownField("a.b"),
            ParseError.UnknownField("a.b"),
        )
        assertNotEquals(
            ParseError.UnknownField("a.b"),
            ParseError.UnknownField("a.c"),
        )
    }

    @Test
    fun `ParseError companion unknownField factory creates UnknownField`() {
        val err = ParseError.UnknownField("meta.extra")
        assertIs<ParseError.UnknownField>(err)
        assertEquals("meta.extra", err.path)
    }

    // ── MissingRequired ──────────────────────────────────────────────────────

    @Test
    fun `MissingRequired message includes path`() {
        val err = ParseError.MissingRequired(path = "flags[0].defaultValue")
        assertTrue(err.message.contains("flags[0].defaultValue"))
    }

    @Test
    fun `MissingRequired accepts custom message`() {
        val err = ParseError.MissingRequired(path = "key", message = "key is mandatory")
        assertEquals("key is mandatory", err.message)
    }

    @Test
    fun `MissingRequired data class equality`() {
        assertEquals(
            ParseError.MissingRequired("a"),
            ParseError.MissingRequired("a"),
        )
        assertNotEquals(
            ParseError.MissingRequired("a"),
            ParseError.MissingRequired("b"),
        )
    }

    @Test
    fun `ParseError companion missingRequired factory creates MissingRequired`() {
        val err = ParseError.missingRequired("flags[2].key")
        assertIs<ParseError.MissingRequired>(err)
        assertEquals("flags[2].key", err.path)
    }

    // ── InvalidValue ─────────────────────────────────────────────────────────

    @Test
    fun `InvalidValue message includes path and reason`() {
        val err = ParseError.InvalidValue(path = "flags[0].rollout", reason = "must be 0.0–100.0")
        assertTrue(err.message.contains("flags[0].rollout"))
        assertTrue(err.message.contains("must be 0.0–100.0"))
    }

    @Test
    fun `InvalidValue accepts custom message`() {
        val err = ParseError.InvalidValue(path = "p", reason = "r", message = "override")
        assertEquals("override", err.message)
    }

    @Test
    fun `InvalidValue data class equality`() {
        assertEquals(
            ParseError.InvalidValue("p", "r"),
            ParseError.InvalidValue("p", "r"),
        )
        assertNotEquals(
            ParseError.InvalidValue("p", "r1"),
            ParseError.InvalidValue("p", "r2"),
        )
    }

    @Test
    fun `ParseError companion invalidValue factory creates InvalidValue`() {
        val err = ParseError.invalidValue("flags[0].rampUp", "must be in 0.0–100.0")
        assertIs<ParseError.InvalidValue>(err)
        assertEquals("flags[0].rampUp", err.path)
        assertEquals("must be in 0.0–100.0", err.reason)
    }

    // ── Result boundary ──────────────────────────────────────────────────────

    @Test
    fun `all new variants survive parseFailure round-trip`() {
        val variants: List<ParseError> = listOf(
            ParseError.unknownField("a.b"),
            ParseError.missingRequired("x.y"),
            ParseError.invalidValue("p.q", "reason"),
        )

        for (variant in variants) {
            val result: Result<Unit> = parseFailure(variant)
            assertTrue(result.isFailure)
            assertEquals(variant, result.parseErrorOrNull())
        }
    }

    // ── Sealed exhaustiveness ─────────────────────────────────────────────────

    @Test
    fun `sealed interface covers all known variants without else branch`() {
        fun classify(err: ParseError): String = when (err) {
            is ParseError.InvalidHexId -> "hex"
            is ParseError.InvalidRollout -> "rollout"
            is ParseError.InvalidVersion -> "version"
            is ParseError.FeatureNotFound -> "featureNotFound"
            is ParseError.FlagNotFound -> "flagNotFound"
            is ParseError.InvalidSnapshot -> "snapshot"
            is ParseError.InvalidJson -> "json"
            is ParseError.UnknownField -> "unknownField"
            is ParseError.MissingRequired -> "missingRequired"
            is ParseError.InvalidValue -> "invalidValue"
            is ParseError.UnknownPredicate -> "unknownPredicate"
            is ParseError.UnversionedExternalRef -> "unversioned"
        }

        assertEquals("unknownField", classify(ParseError.unknownField("x")))
        assertEquals("missingRequired", classify(ParseError.missingRequired("x")))
        assertEquals("invalidValue", classify(ParseError.invalidValue("x", "r")))
    }
}
