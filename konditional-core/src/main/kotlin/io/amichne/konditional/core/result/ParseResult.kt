package io.amichne.konditional.core.result

/**
 * A parseUnsafe result that represents either successful parsing or a domain-specific error.
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
     * Successful parseUnsafe result containing the parsed value.
     */
    @ConsistentCopyVisibility
    data class Success<T> @PublishedApi internal constructor(val value: T) : ParseResult<T>

    /**
     * Failed parseUnsafe result containing structured error information.
     */
    @ConsistentCopyVisibility
    data class Failure @PublishedApi internal constructor(val error: ParseError) : ParseResult<Nothing>

    companion object {
        /**
         * Create a successful parse result.
         */
        fun <T> success(value: T): ParseResult<T> = Success(value)

        /**
         * Create a failed parse result.
         */
        fun failure(error: ParseError): ParseResult<Nothing> = Failure(error)
    }
}

/**
 * Returns the value if successful, or throws an exception if failed.
 * Useful for tests or when you want to fail fast.
 */
fun <T> ParseResult<T>.getOrThrow(): T = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> throw IllegalStateException(error.message)
}
