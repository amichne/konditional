file=konditional-core/src/main/kotlin/io/amichne/konditional/internal/builders/AxisValuesBuilder.kt
package=io.amichne.konditional.internal.builders
imports=io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.dsl.AxisCatalogScope,io.amichne.konditional.core.dsl.AxisValuesScope,io.amichne.konditional.core.dsl.KonditionalDsl,io.amichne.konditional.core.registry.AxisCatalog
type=io.amichne.konditional.internal.builders.AxisValuesBuilder|kind=class|decl=internal class AxisValuesBuilder( override val axisCatalog: AxisCatalog? = null, val map: MutableMap<String, MutableSet<AxisValue<*>>> = mutableMapOf(), ) :
