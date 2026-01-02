package io.amichne.konditional.configmetadata.dsl

import io.amichne.konditional.configmetadata.contract.BindingType
import io.amichne.konditional.configmetadata.contract.ConfigMetadata
import io.amichne.konditional.configmetadata.descriptor.ValueDescriptor

@DslMarker
annotation class ConfigMetadataDsl

@ConfigMetadataDsl
class ConfigMetadataBuilder {
    private val bindings = linkedMapOf<String, BindingType>()
    private val descriptors = linkedMapOf<BindingType, ValueDescriptor>()

    fun bind(
        pointerTemplate: String,
        type: BindingType,
    ) {
        require(pointerTemplate.isNotBlank()) { "Pointer template must be non-blank." }
        bindings[pointerTemplate] = type
    }

    fun describe(
        type: BindingType,
        descriptor: ValueDescriptor,
    ) {
        descriptors[type] = descriptor
    }

    fun build(): ConfigMetadata =
        ConfigMetadata(
            bindings = bindings.toMap(),
            descriptors = descriptors.toMap(),
        )
}

fun configMetadata(block: ConfigMetadataBuilder.() -> Unit): ConfigMetadata =
    ConfigMetadataBuilder().apply(block).build()
