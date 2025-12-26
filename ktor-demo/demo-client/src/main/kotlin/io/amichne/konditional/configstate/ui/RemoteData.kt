package io.amichne.konditional.configstate.ui

sealed interface RemoteData<out T> {
    data object Idle : RemoteData<Nothing>

    data object Loading : RemoteData<Nothing>

    data class Loaded<T>(
        val value: T,
    ) : RemoteData<T>

    data class Failed(
        val message: String,
    ) : RemoteData<Nothing>
}

