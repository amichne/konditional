package io.amichne.konditional.core.result

/**
 * A parse result that represents either successful parsing or a domain-specific error.
 *
 * This type follows the "Parse, Don't Validate" principle:
 * - Parse once at system boundaries into refined domain types
 * - Never throw exceptions - return typed results
 * - Make illegal states unrepresentable
 *
 * Use `fold()` to transform into your preferred error handling type (Result, Either, Outcome, etc.)
 *
 * @param T the successfully parsed value type
 */
sealed interface ParseResult<out T> {
    /**
     * Successful parse result containing the parsed value.
     */
    data class Success<T>(val value: T) : ParseResult<T>

    /**
     * Failed parse result containing structured error information.
     */
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
