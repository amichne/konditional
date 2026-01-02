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
    onFailure: (ParseError) -> R,
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
 * Returns true if this is a successful parseUnsafe result.
 */
fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success

/**
 * Returns true if this is a failed parseUnsafe result.
 */
fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure

/**
 * Execute an action if the result is successful, then return the result.
 *
 * Useful for logging or side effects without changing the result:
 * ```kotlin
 * parseResult
 *     .onSuccess { value -> logger.info("Parsed successfully: $value") }
 *     .getOrElse { default }
 * ```
 */
inline fun <T> ParseResult<T>.onSuccess(action: (T) -> Unit): ParseResult<T> {
    if (this is ParseResult.Success) {
        action(value)
    }
    return this
}

/**
 * Execute an action if the result is a failure, then return the result.
 *
 * Useful for logging errors or side effects without changing the result:
 * ```kotlin
 * parseResult
 *     .onFailure { error -> logger.error("Parse failed: ${error.message}") }
 *     .recover { defaultValue }
 * ```
 */
inline fun <T> ParseResult<T>.onFailure(action: (ParseError) -> Unit): ParseResult<T> {
    if (this is ParseResult.Failure) {
        action(error)
    }
    return this
}

/**
 * Recover from a failure by providing a fallback value.
 *
 * Unlike `getOrElse`, this always returns a value (not a ParseResult):
 * ```kotlin
 * val config = io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec.decode(json)
 *     .onFailure { error -> logger.error(error.message) }
 *     .recover { Configuration.empty() }
 * ```
 */
inline fun <T> ParseResult<T>.recover(transform: (ParseError) -> T): T = when (this) {
    is ParseResult.Success -> value
    is ParseResult.Failure -> transform(error)
}

/**
 * Convert ParseResult to Kotlin's Result type.
 * Wraps ParseError in ParseException for compatibility with Result.
 */
fun <T> ParseResult<T>.toResult(): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(ParseException(it)) }
)
