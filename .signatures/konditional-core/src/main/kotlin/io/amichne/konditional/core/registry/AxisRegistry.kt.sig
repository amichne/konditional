file=konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisRegistry.kt
package=io.amichne.konditional.core.registry
imports=io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,java.util.concurrent.ConcurrentHashMap,kotlin.reflect.KClass
type=io.amichne.konditional.core.registry.AxisRegistry|kind=object|decl=internal object AxisRegistry
fields:
- private val byId: MutableMap<String, Axis<*>>
- private val byValueClass: MutableMap<KClass<*>, Axis<*>>
methods:
- internal fun register(axis: Axis<*>)
- fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T>
- internal fun <T> axisForOrThrow(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T>
- internal fun axisIdsFor(axisId: String): Set<String>
- internal fun axisIdsFor(axis: Axis<*>): Set<String>
- internal fun <T> axisIdsFor(type: KClass<out T>): Set<String> where T : AxisValue<T>, T : Enum<T>
