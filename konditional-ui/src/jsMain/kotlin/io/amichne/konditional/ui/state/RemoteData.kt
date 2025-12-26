package io.amichne.konditional.ui.state

/**
 * Represents the state of asynchronously loaded data.
 *
 * This sealed interface provides a type-safe way to handle loading states,
 * errors, and successful data in React components.
 */
sealed interface RemoteData<out T> {
    /**
     * Initial state before any request has been made.
     */
    data object Idle : RemoteData<Nothing>

    /**
     * Request is in progress.
     */
    data object Loading : RemoteData<Nothing>

    /**
     * Request succeeded with data.
     */
    data class Loaded<T>(val value: T) : RemoteData<T>

    /**
     * Request failed with an error.
     */
    data class Failed(val error: Throwable) : RemoteData<Nothing>
}

/**
 * Maps the loaded value to a new type.
 */
inline fun <T, R> RemoteData<T>.map(transform: (T) -> R): RemoteData<R> =
    when (this) {
        is RemoteData.Idle -> RemoteData.Idle
        is RemoteData.Loading -> RemoteData.Loading
        is RemoteData.Loaded -> RemoteData.Loaded(transform(value))
        is RemoteData.Failed -> this
    }

/**
 * Returns the loaded value or null.
 */
fun <T> RemoteData<T>.getOrNull(): T? =
    when (this) {
        is RemoteData.Loaded -> value
        else -> null
    }

/**
 * Returns the loaded value or a default.
 */
fun <T> RemoteData<T>.getOrElse(default: () -> T): T =
    when (this) {
        is RemoteData.Loaded -> value
        else -> default()
    }

/**
 * Returns true if data is currently loading.
 */
fun <T> RemoteData<T>.isLoading(): Boolean = this is RemoteData.Loading

/**
 * Returns true if data has been successfully loaded.
 */
fun <T> RemoteData<T>.isLoaded(): Boolean = this is RemoteData.Loaded

/**
 * Returns true if loading has failed.
 */
fun <T> RemoteData<T>.isFailed(): Boolean = this is RemoteData.Failed

/**
 * Returns the error if failed, null otherwise.
 */
fun <T> RemoteData<T>.errorOrNull(): Throwable? =
    when (this) {
        is RemoteData.Failed -> error
        else -> null
    }
