package io.amichne.konditional.core.result

/**
 * Explicit result type for trust-boundary parsing in the enterprise modules.
 */
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>

    data class Failure(val error: ParseError) : ParseResult<Nothing>
}

fun <T> Result<T>.toParseResult(): ParseResult<T> =
    fold(
        onSuccess = { ParseResult.Success(it) },
        onFailure = { throwable ->
            ParseResult.Failure(
                throwable.parseErrorOrNull()
                    ?: ParseError.invalidSnapshot(throwable.message ?: "Unknown parse failure"),
            )
        },
    )
