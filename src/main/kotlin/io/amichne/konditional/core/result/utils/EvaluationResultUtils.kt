package io.amichne.konditional.core.result.utils

import io.amichne.konditional.core.result.EvaluationResult
import io.amichne.konditional.core.result.FlagEvaluationException
import io.amichne.konditional.core.result.FlagNotFoundException

/**
 * Transform this result into any type by providing handlers for each case.
 *
 * This is an inline function with zero runtime overhead.
 * Use this to adapt EvaluationResult to your error handling system:
 * ```kotlin
 * context.evaluateSafe(MY_FLAG).fold(
 *     onSuccess = { Outcome.Success(it) },
 *     onFlagNotFound = { key -> Outcome.Failure(MyError.FlagNotRegistered(key)) },
 *     onEvaluationError = { key, error -> Outcome.Failure(MyError.EvaluationFailed(key, error)) }
 * )
 * ```
 */
inline fun <S, R> EvaluationResult<S>.fold(
    onSuccess: (S) -> R,
    onFlagNotFound: (String) -> R,
    onEvaluationError: (String, Throwable) -> R
): R = when (this) {
    is EvaluationResult.Success -> onSuccess(value)
    is EvaluationResult.FlagNotFound -> onFlagNotFound(key)
    is EvaluationResult.EvaluationError -> onEvaluationError(key, error)
}

/**
 * Transform the success value while preserving errors.
 * ```kotlin
 * evaluationResult.map { it.toString() }
 * ```
 */
inline fun <S, R> EvaluationResult<S>.map(transform: (S) -> R): EvaluationResult<R> = when (this) {
    is EvaluationResult.Success -> EvaluationResult.Success(transform(value))
    is EvaluationResult.FlagNotFound -> this
    is EvaluationResult.EvaluationError -> this
}

/**
 * Get the value if successful, null otherwise.
 * Use this when you want to ignore the error details.
 */
fun <S> EvaluationResult<S>.getOrNull(): S? = when (this) {
    is EvaluationResult.Success -> value
    is EvaluationResult.FlagNotFound, is EvaluationResult.EvaluationError -> null
}

/**
 * Get the value if successful, or a default value if failed.
 */
fun <S> EvaluationResult<S>.getOrDefault(default: S): S = when (this) {
    is EvaluationResult.Success -> value
    is EvaluationResult.FlagNotFound, is EvaluationResult.EvaluationError -> default
}

/**
 * Get the value if successful, or compute a default based on the error.
 */
inline fun <S> EvaluationResult<S>.getOrElse(onError: (EvaluationResult<Nothing>) -> S): S = when (this) {
    is EvaluationResult.Success -> value
    is EvaluationResult.FlagNotFound -> onError(this)
    is EvaluationResult.EvaluationError -> onError(this)
}

/**
 * Returns true if this is a successful evaluation.
 */
fun <S> EvaluationResult<S>.isSuccess(): Boolean = this is EvaluationResult.Success

/**
 * Returns true if this is a failure (either FlagNotFound or EvaluationError).
 */
fun <S> EvaluationResult<S>.isFailure(): Boolean = !isSuccess()

/**
 * Convert EvaluationResult to Kotlin's Result type.
 *
 * Maps both FlagNotFound and EvaluationError to Result.failure.
 * If you need to distinguish between these cases, use `fold()` instead.
 */
fun <S> EvaluationResult<S>.toResult(): Result<S> = fold(
    onSuccess = { Result.success(it) },
    onFlagNotFound = { Result.failure(FlagNotFoundException(it)) },
    onEvaluationError = { key, error -> Result.failure(FlagEvaluationException(key, error)) }
)
