package io.amichne.konditional.core.result

/**
 * Exception thrown when flag evaluation fails.
 */
class FlagEvaluationException(val key: String, cause: Throwable) :
    RuntimeException("Flag evaluation failed: $key", cause)
