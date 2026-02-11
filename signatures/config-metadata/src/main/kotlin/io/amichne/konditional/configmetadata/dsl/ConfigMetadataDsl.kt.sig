file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt
package=io.amichne.konditional.configmetadata.dsl
imports=io.amichne.konditional.configmetadata.contract.BindingType,io.amichne.konditional.configmetadata.contract.ConfigMetadata,io.amichne.konditional.configmetadata.descriptor.ValueDescriptor
type=io.amichne.konditional.configmetadata.dsl.ConfigMetadataDsl|kind=class|decl=annotation class ConfigMetadataDsl
type=io.amichne.konditional.configmetadata.dsl.ConfigMetadataBuilder|kind=class|decl=class ConfigMetadataBuilder
fields:
- private val bindings
- private val descriptors
methods:
- fun bind( pointerTemplate: String, type: BindingType, )
- fun describe( type: BindingType, descriptor: ValueDescriptor, )
- fun build(): ConfigMetadata
