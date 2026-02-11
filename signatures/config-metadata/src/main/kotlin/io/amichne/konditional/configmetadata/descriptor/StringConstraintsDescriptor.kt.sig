file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/StringConstraintsDescriptor.kt
package=io.amichne.konditional.configmetadata.descriptor
imports=io.amichne.konditional.configmetadata.ui.UiHints
type=io.amichne.konditional.configmetadata.descriptor.StringConstraintsDescriptor|kind=class|decl=data class StringConstraintsDescriptor( override val uiHints: UiHints, val minLength: Int? = null, val maxLength: Int? = null, val pattern: String? = null, val suggestions: List<String>? = null, ) : ValueDescriptor
fields:
- override val kind: ValueDescriptor.Kind
