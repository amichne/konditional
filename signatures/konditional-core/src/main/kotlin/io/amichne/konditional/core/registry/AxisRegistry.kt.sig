file=konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisRegistry.kt
package=io.amichne.konditional.core.registry
imports=io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,java.util.concurrent.ConcurrentHashMap,kotlin.reflect.KClass
type=io.amichne.konditional.core.registry.AxisRegistry|kind=object|decl=internal object AxisRegistry
type=io.amichne.konditional.core.registry.AxisEntry|kind=class|decl=private data class AxisEntry( val id: String, val valueClass: KClass<*>, val isImplicit: Boolean, )
fields:
- private val byId: MutableMap<String, AxisEntry>
- private val byValueClass: MutableMap<KClass<*>, AxisEntry>
- private val idsByValueClass: MutableMap<KClass<*>, MutableSet<String>>
methods:
- internal fun register(axis: Axis<*>)
- fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T>
- internal fun <T> axisForOrRegister(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T>
- internal fun axisIdsFor(axisId: String): Set<String>
- internal fun axisIdsFor(axis: Axis<*>): Set<String>
- internal fun <T> axisIdsFor(type: KClass<out T>): Set<String> where T : AxisValue<T>, T : Enum<T>
- private fun <T> implicitAxisId(type: KClass<out T>): String where T : AxisValue<T>, T : Enum<T>
- private fun <T> registerImplicit(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T>
- private fun axisIdsForValueClass(valueClass: KClass<*>): Set<String>
- private fun rememberAxisId( valueClass: KClass<*>, axisId: String, )
