package io.amichne.konditional.configstate

import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.serialization.SnapshotSerializer

object ConfigurationStateFactory {
    fun from(
        configuration: Configuration,
        supportedValues: SupportedValues = ConfigurationStateSupportedValuesCatalog.current(),
    ): ConfigurationStateResponse =
        ConfigurationStateResponse(
            currentState = SnapshotSerializer.toSerializableSnapshot(configuration),
            supportedValues = supportedValues,
        )
}

