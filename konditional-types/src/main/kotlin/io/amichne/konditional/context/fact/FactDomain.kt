package io.amichne.konditional.context.fact

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import kotlin.reflect.KClass

/** Stable wire representation of immutable fact values. Both functions must be pure inverses. */
class FactDomain<T : Any> @PublishedApi internal constructor(
    private val valueType: KClass<T>,
    internal val encode: (T) -> String,
    internal val decode: (String) -> ParseResult<T>,
) {
    internal fun typed(value: Any): T = valueType.javaObjectType.cast(value)

    companion object {
        /** Declares a domain. Invalid external operands must return a typed failure from [decode]. */
        inline operator fun <reified T : Any> invoke(
            noinline encode: (T) -> String,
            noinline decode: (String) -> ParseResult<T>,
        ): FactDomain<T> = FactDomain(T::class, encode, decode)

        /** Declares a finite enum domain. Duplicate wire identifiers are a declaration error. */
        inline fun <reified T : Enum<T>> enumeration(
            entries: Collection<T>,
            noinline id: (T) -> String,
        ): FactDomain<T> {
            val declared = entries.toSet()
            require(declared.size == entries.size) { "Duplicate enum fact entry" }
            require(declared == enumValues<T>().toSet()) { "Incomplete enum fact domain" }
            val identifiers = declared.associateWith(id)
            val byId = identifiers.entries.associate { (value, raw) -> raw to value }
            require(byId.size == declared.size) { "Duplicate fact operand identifier" }
            return FactDomain(identifiers::getValue) { raw ->
                byId[raw]?.let { ParseResult.Success(it) }
                    ?: ParseResult.Failure(ParseError.invalidValue("operand", "Unknown enum identifier '$raw'"))
            }
        }
    }
}
