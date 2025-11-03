package io.amichne.konditional.core.result.utils

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseException
import io.amichne.konditional.core.result.ParseResult

/**
 * Transform this result into any type by providing success and failure handlers.
 *
 * This is an inline function with zero runtime overhead.
 * Use this to adapt ParseResult to your error handling system:
 * ```kotlin
 * parseResult.fold(
 *     onSuccess = { Result.success(it) },
 *     onFailure = { error -> Result.failure(ParseException(error)) }
 * )
 * ```
 */
inline fun <T, R> ParseResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (ParseError) -> R
): R = when (this) {
    is ParseResult.Success -> onSuccess(value)
    is ParseResult.Failure -> onFailure(error)
}

/**
 * Transform the success value while preserving failures.
 * ```kotlin
 * parseResult.map { it.toString() }
 * ```
 */
inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R> = when (this) {
    is ParseResult.Success -> ParseResult.Success(transform(value))
    is ParseResult.Failure -> this
}

/**
 * Chain dependent parsing operations.
 * ```kotlin
 * parseHexId(input).flatMap { hexId ->
 *     parseVersion(hexId.value)
 * }
 * ```
 */
inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> = when (this) {
    is ParseResult.Success -> transform(value)
    is ParseResult.Failure -> this
}

/**
 * Get the value if successful, null otherwise.
 * Use this when null is an acceptable fallback.
 */
fun <T> ParseResult<T>.getOrNull(): T? = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> null
}

/**
 * Get the value if successful, or a default value if failed.
 */
fun <T> ParseResult<T>.getOrDefault(default: T): T = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> default
}

/**
 * Get the value if successful, or compute a default from the error.
 */
inline fun <T> ParseResult<T>.getOrElse(onFailure: (ParseError) -> T): T = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> onFailure(error)
}

/**
 * Returns true if this is a successful parse result.
 */
fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success

/**
 * Returns true if this is a failed parse result.
 */
fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure

/**
 * Convert ParseResult to Kotlin's Result type.
 * Wraps ParseError in ParseException for compatibility with Result.
 */
fun <T> ParseResult<T>.toResult(): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(ParseException(it)) }
)
