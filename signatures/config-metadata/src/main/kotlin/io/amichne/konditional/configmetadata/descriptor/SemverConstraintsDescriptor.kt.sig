file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/SemverConstraintsDescriptor.kt
package=io.amichne.konditional.configmetadata.descriptor
imports=io.amichne.konditional.configmetadata.ui.UiHints
type=io.amichne.konditional.configmetadata.descriptor.SemverConstraintsDescriptor|kind=class|decl=data class SemverConstraintsDescriptor( override val uiHints: UiHints, val minimum: String, val allowAnyAboveMinimum: Boolean = true, val pattern: String? = null, ) : ValueDescriptor
fields:
- override val kind: ValueDescriptor.Kind
