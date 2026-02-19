file=konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt
package=io.amichne.konditional.context.axis
imports=kotlin.reflect.KClass
type=io.amichne.konditional.context.axis.AxisValues|kind=class|decl=class AxisValues internal constructor( private val values: Map<String, Set<AxisValue<*>>>, ) : Set<AxisValue<*>> by values.values.flatten().toSet()
methods:
- internal operator fun get(axisId: String): Set<AxisValue<*>>
- operator fun <T> get(axis: Axis<T>): Set<T> where T : AxisValue<T>, T : Enum<T>
- internal fun <T> valuesFor(type: KClass<out T>): Set<T> where T : AxisValue<T>, T : Enum<T>
- override fun equals(other: Any?): Boolean
- override fun hashCode(): Int
- override fun toString(): String
