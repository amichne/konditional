package io.amichne.konditional.json

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.fixtures.TestNamespace
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.fixtures.RetryPolicy
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnapshotValueTypeTest {
    enum class Mode {
        FIRST { override fun toString() = "first" },
        SECOND,
    }

    data class Label(override val value: String) : Konstrained.Primitive.String

    @Test
    fun enumClassNamesResolveAgainstTheDeclaredEnumIncludingLegacyEntryNames() {
        val namespace = object : TestNamespace("json-enum-witness") {
            val mode by enum<Mode, Context>(default = Mode.FIRST)
        }
        fun snapshot(value: Mode, className: String) =
            snapshotDocument("""{"flags":[{"key":"${namespace.mode.id.plainId}","defaultValue":{
                "type":"ENUM","value":"${value.name}","enumClassName":"$className"}}]}""")

        assertIs<ParseResult.Success<*>>(namespace.fromJson(snapshot(Mode.FIRST, Mode.FIRST.javaClass.name)))
        assertIs<ParseResult.Success<*>>(namespace.fromJson(snapshot(Mode.SECOND, Mode::class.java.name)))
        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(snapshot(Mode.FIRST, "unrelated.Enum")))
        assertIs<ParseError.InvalidSnapshot>(failure.error)
        assertEquals(Mode.SECOND, namespace.mode.evaluate(TestContext()))
    }

    @Test
    fun konstrainedValuesMustMatchTheDeclaredClassAndRepresentation() {
        val namespace = object : TestNamespace("json-custom-witness") {
            val label by custom<Label, Context>(default = Label("old"))
            val retry by custom<RetryPolicy, Context>(default = RetryPolicy())
        }
        val valid = namespace.toJson()
        assertIs<ParseResult.Success<*>>(namespace.fromJson(valid))
        for (declared in listOf(Label::class.java.name, RetryPolicy::class.java.name)) {
            val failure = assertIs<ParseResult.Failure>(namespace.fromJson(valid.replace(declared, "unrelated.Class")))
            assertIs<ParseError.InvalidSnapshot>(failure.error)
        }
        val wrongRepresentation = """{"flags":[{"key":"${namespace.retry.id.plainId}","defaultValue":{
            "type":"KONSTRAINED_PRIMITIVE","value":"x","konstrainedClassName":"${RetryPolicy::class.java.name}"}}]}"""
        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(snapshotDocument(wrongRepresentation)))
        assertIs<ParseError.InvalidSnapshot>(failure.error)
        assertEquals(RetryPolicy(), namespace.retry.evaluate(TestContext()))
        assertEquals(Label("old"), namespace.label.evaluate(TestContext()))
    }

    @Test
    fun defaultsAndStaticRulesMustMatchTheDeclaredBoolean() {
        val namespace = object : TestNamespace("json-value-types") {
            val enabled by boolean<Context>(default = false)
        }
        val boolean = """{"type":"BOOLEAN","value":true}"""
        val string = """{"type":"STRING","value":"wrong"}"""
        fun snapshot(default: String, rules: String = "[]") =
            snapshotDocument("""{"flags":[{"key":"${namespace.enabled.id.plainId}","defaultValue":$default,"rules":$rules}]}""")

        assertIs<ParseResult.Success<*>>(namespace.fromJson(snapshot(boolean)))
        for (invalid in listOf(snapshot(string), snapshot(boolean, """[{"value":$string,"conditions":[]}]"""))) {
            val failure = assertIs<ParseResult.Failure>(namespace.fromJson(invalid))
            assertIs<ParseError.InvalidSnapshot>(failure.error)
            assertEquals(true, namespace.enabled.evaluate(TestContext()))
        }
    }
}
