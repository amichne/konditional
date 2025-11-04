package io.amichne.konditional.core.result

/**
 * Evaluation result that represents the outcome of evaluating a feature flag.
 *
 * This type follows the "Parse, Don't Validate" principle by making all evaluation
 * outcomes explicit and type-safe. Unlike throwing APIs, this allows precise error handling
 * without catching generic exceptions.
 *
 * Use `fold()` to transform into your preferred error handling type (Result, Either, Outcome, etc.)
 *
 * @param S the type of the successfully evaluated value
 */
sealed interface EvaluationResult<out S> {
    /**
     * Successfully evaluated the flag and got a value.
     */
    data class Success<S>(val value: S) : EvaluationResult<S>

    /**
     * The flag was not found in the registry.
     * This typically means the flag hasn't been registered yet.
     */
    data class FlagNotFound(val key: String) : EvaluationResult<Nothing>

    /**
     * An error occurred while evaluating the flag.
     * This is distinct from FlagNotFound - the flag exists but evaluation failed.
     */
    data class EvaluationError(val key: String, val error: Throwable) : EvaluationResult<Nothing>
}
