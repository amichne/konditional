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

/**
 * Returns the value if successful, or throws an exception if failed.
 * Useful for tests or when you want to fail fast.
 */
fun <T> ParseResult<T>.getOrThrow(): T = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> throw IllegalStateException(error.message)
}

/**
 * Returns the value if successful, or null if failed.
 */
fun <T> ParseResult<T>.getOrNull(): T? = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> null
}

/**
 * Returns true if this is a Success result.
 */
fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success

/**
 * Returns true if this is a Failure result.
 */
fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure
