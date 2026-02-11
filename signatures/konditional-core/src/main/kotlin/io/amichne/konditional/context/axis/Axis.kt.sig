file=konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt
package=io.amichne.konditional.context.axis
imports=kotlin.reflect.KClass
type=io.amichne.konditional.context.axis.Axis|kind=class|decl=class Axis<T> private constructor( val id: String, val valueClass: KClass<out T>, val isImplicit: Boolean = false, autoRegister: Boolean = true, ) where T : AxisValue<T>, T : Enum<T>
type=io.amichne.konditional.context.axis.Delegate|kind=interface|decl=interface Delegate<T> where T : AxisValue<T>, T : Enum<T>
fields:
- val id: String
- val valueClass: KClass<out T>
- val isImplicit: Boolean
- val autoRegister: Boolean
methods:
- override fun equals(other: Any?): Boolean
- override fun hashCode(): Int
- override fun toString(): String
