file=konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisDefinition.kt
package=io.amichne.konditional.context.axis
imports=kotlin.reflect.KClass
type=io.amichne.konditional.context.axis.AxisDefinition|kind=class|decl=open class AxisDefinition<T>( override val id: String, override val valueClass: KClass<out T>, open override val isImplicit: Boolean = false, open override val autoRegister: Boolean = true, ) : Axis.Delegate<T> where T : AxisValue<T>, T : Enum<T>
fields:
- val axis: Axis<T>
methods:
- override fun toString(): String
