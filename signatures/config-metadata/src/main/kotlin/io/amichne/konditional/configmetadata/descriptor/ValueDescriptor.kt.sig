file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/ValueDescriptor.kt
package=io.amichne.konditional.configmetadata.descriptor
imports=io.amichne.konditional.configmetadata.ui.UiHints
type=io.amichne.konditional.configmetadata.descriptor.ValueDescriptor|kind=interface|decl=sealed interface ValueDescriptor
type=io.amichne.konditional.configmetadata.descriptor.Kind|kind=enum|decl=enum class Kind
fields:
- val kind: Kind
- val uiHints: UiHints
