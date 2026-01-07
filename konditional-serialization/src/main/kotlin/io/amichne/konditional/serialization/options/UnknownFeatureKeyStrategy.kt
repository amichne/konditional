package io.amichne.konditional.serialization.options

sealed interface UnknownFeatureKeyStrategy {
    data object Fail : UnknownFeatureKeyStrategy

    data object Skip : UnknownFeatureKeyStrategy
}
