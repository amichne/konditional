file=konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisCatalog.kt
package=io.amichne.konditional.core.registry
imports=io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,java.util.concurrent.ConcurrentHashMap,kotlin.reflect.KClass
type=io.amichne.konditional.core.registry.AxisCatalog|kind=class|decl=class AxisCatalog
methods:
- fun register(axis: Axis<*>)
- fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T>
- fun <T> axisForOrThrow(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T>
