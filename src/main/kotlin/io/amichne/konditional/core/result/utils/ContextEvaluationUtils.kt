package io.amichne.konditional.core.result.utils

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.SingletonFlagRegistry
import io.amichne.konditional.core.result.EvaluationResult
import io.amichne.konditional.core.result.FlagEvaluationException
import io.amichne.konditional.core.result.FlagNotFoundException

/**
 * Evaluate a flag with explicit error handling that never throws.
 *
 * This is the primary evaluation API. It returns a typed result that distinguishes between:
 * - Successfully getting a value
 * - Flag not being registered
 * - Evaluation throwing an exception
 *
 * Usage:
 * ```kotlin
 * when (val result = context.evaluateSafe(MY_FLAG)) {
 *     is EvaluationResult.Success -> handleValue(result.value)
 *     is EvaluationResult.FlagNotFound -> handleMissingFlag(result.key)
 *     is EvaluationResult.EvaluationError -> handleError(result.key, result.error)
 * }
 * ```
 *
 * Or adapt to your error type:
 * ```kotlin
 * context.evaluateSafe(MY_FLAG).fold(
 *     onSuccess = { Outcome.Success(it) },
 *     onFlagNotFound = { Outcome.Failure(MyError.FlagNotFound(it)) },
 *     onEvaluationError = { key, err -> Outcome.Failure(MyError.Failed(key, err)) }
 * )
 * ```
 *
 * @param key the conditional key identifying the flag
 * @param registry the flag registry to lookup the flag (defaults to SingletonFlagRegistry)
 * @return typed result that never throws
 */
fun <S : Any, C : Context> C.evaluateSafe(
    key: Conditional<S, C>,
    registry: FlagRegistry = SingletonFlagRegistry
): EvaluationResult<S> =
    registry.getFlag(key)?.let { flag ->
        runCatching { flag.evaluate(this) }
            .fold(
                onSuccess = { EvaluationResult.Success(it) },
                onFailure = { EvaluationResult.EvaluationError(key.key, it) }
            )
    } ?: EvaluationResult.FlagNotFound(key.key)

/**
 * Evaluate a flag, returning null if not found or evaluation fails.
 *
 * Use this when:
 * - You don't need to distinguish between FlagNotFound and EvaluationError
 * - Null is an acceptable fallback
 * - You're in a context where nullable types work well
 *
 * If you need to distinguish error cases, use `evaluateSafe()` instead.
 *
 * ```kotlin
 * val feature: String? = context.evaluateOrNull(MY_FLAG)
 * if (feature != null) {
 *     // use feature
 * }
 * ```
 */
fun <S : Any, C : Context> C.evaluateOrNull(
    key: Conditional<S, C>,
    registry: FlagRegistry = SingletonFlagRegistry
): S? = evaluateSafe(key, registry).getOrNull()

/**
 * Evaluate a flag, returning a default value if not found or evaluation fails.
 *
 * Use this when:
 * - You have a sensible default value
 * - You don't need error details
 * - You want the simplest possible API
 *
 * If you need to distinguish error cases, use `evaluateSafe()` instead.
 *
 * ```kotlin
 * val feature: String = context.evaluateOrDefault(MY_FLAG, default = "off")
 * ```
 */
fun <S : Any, C : Context> C.evaluateOrDefault(
    key: Conditional<S, C>,
    default: S,
    registry: FlagRegistry = SingletonFlagRegistry
): S = evaluateSafe(key, registry).getOrDefault(default)

/**
 * Evaluate a flag, throwing an exception if not found or evaluation fails.
 *
 * ⚠️ Use this sparingly! Prefer `evaluateSafe()` for explicit error handling.
 *
 * Only use this when:
 * - The flag not existing is truly an exceptional (programmer error) case
 * - You're in a context that already uses exceptions
 * - You want fail-fast behavior
 *
 * ```kotlin
 * val feature: String = context.evaluateOrThrow(MY_FLAG)
 * ```
 *
 * @throws FlagNotFoundException if the flag is not registered
 * @throws FlagEvaluationException if evaluation throws an exception
 */
fun <S : Any, C : Context> C.evaluateOrThrow(
    key: Conditional<S, C>,
    registry: FlagRegistry = SingletonFlagRegistry
): S = evaluateSafe(key, registry).fold(
    onSuccess = { it },
    onFlagNotFound = { throw FlagNotFoundException(it) },
    onEvaluationError = { flagKey, error -> throw FlagEvaluationException(flagKey, error) }
)
