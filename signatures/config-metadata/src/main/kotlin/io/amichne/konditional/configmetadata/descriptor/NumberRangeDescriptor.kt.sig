file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/NumberRangeDescriptor.kt
package=io.amichne.konditional.configmetadata.descriptor
imports=io.amichne.konditional.configmetadata.ui.UiHints
type=io.amichne.konditional.configmetadata.descriptor.NumberRangeDescriptor|kind=class|decl=data class NumberRangeDescriptor( override val uiHints: UiHints, val min: Double, val max: Double, val step: Double, val unit: String? = null, ) : ValueDescriptor
fields:
- override val kind: ValueDescriptor.Kind
