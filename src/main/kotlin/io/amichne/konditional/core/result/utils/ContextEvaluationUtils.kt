package io.amichne.konditional.core.result.utils

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Feature
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
 * The feature's featureModule-scoped registry is automatically used.
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
 * @return typed result that never throws
 */
fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context, M : io.amichne.konditional.core.FeatureModule> C.evaluateSafe(
    key: Feature<S, T, C, M>
): EvaluationResult<T> =
    key.registry.featureFlag(key)?.let { flag ->
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
 * The feature's featureModule-scoped registry is automatically used.
 *
 * ```kotlin
 * val feature: String? = context.evaluateOrNull(MY_FLAG)
 * if (feature != null) {
 *     // use feature
 * }
 * ```
 */
fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context, M : io.amichne.konditional.core.FeatureModule> C.evaluateOrNull(
    key: Feature<S, T, C, M>
): T? = evaluateSafe(key).getOrNull()

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
 * The feature's featureModule-scoped registry is automatically used.
 *
 * ```kotlin
 * val feature: String = context.evaluateOrDefault(MY_FLAG, default = "off")
 * ```
 */
fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context, M : io.amichne.konditional.core.FeatureModule> C.evaluateOrDefault(
    key: Feature<S, T, C, M>,
    default: T
): T = evaluateSafe(key).getOrDefault(default)

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
 * The feature's featureModule-scoped registry is automatically used.
 *
 * ```kotlin
 * val feature: String = context.evaluateOrThrow(MY_FLAG)
 * ```
 *
 * @throws FlagNotFoundException if the flag is not registered
 * @throws FlagEvaluationException if evaluation throws an exception
 */
fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context, M : io.amichne.konditional.core.FeatureModule> C.evaluateOrThrow(
    key: Feature<S, T, C, M>
): T = evaluateSafe(key).fold(
    onSuccess = { it },
    onFlagNotFound = { throw FlagNotFoundException(it) },
    onEvaluationError = { flagKey, error -> throw FlagEvaluationException(flagKey, error) }
)
