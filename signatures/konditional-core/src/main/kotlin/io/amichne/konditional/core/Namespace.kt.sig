file=konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.dsl.FlagScope,io.amichne.konditional.core.features.BooleanFeature,io.amichne.konditional.core.features.DoubleFeature,io.amichne.konditional.core.features.EnumFeature,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.features.IntFeature,io.amichne.konditional.core.features.KotlinClassFeature,io.amichne.konditional.core.features.StringFeature,io.amichne.konditional.core.registry.NamespaceRegistry,io.amichne.konditional.core.registry.NamespaceRegistryFactories,io.amichne.konditional.core.registry.NamespaceRegistryRuntime,io.amichne.konditional.core.spi.FeatureRegistrationHooks,io.amichne.konditional.core.types.Konstrained,io.amichne.konditional.internal.builders.FlagBuilder,io.amichne.konditional.values.IdentifierEncoding.SEPARATOR,java.util.UUID,kotlin.reflect.KProperty,org.jetbrains.annotations.TestOnly
type=io.amichne.konditional.core.Namespace|kind=class|decl=open class Namespace( val id: String, @property:KonditionalInternalApi val registry: NamespaceRegistry = NamespaceRegistryFactories.default(id), /** * Seed used to construct stable [io.amichne.konditional.values.FeatureId] values for features. * * By default this is the namespace [id], which is appropriate for "real" namespaces that are intended * to be federated and stable within an application. * * Test-only/ephemeral namespaces should provide a per-instance unique seed to avoid collisions. */ @PublishedApi internal val identifierSeed: String = id, ) : NamespaceRegistry by registry
type=io.amichne.konditional.core.TestNamespaceFacade|kind=class|decl=abstract class TestNamespaceFacade(id: String) : Namespace( id = id, registry = NamespaceRegistryFactories.default(id), identifierSeed = UUID.randomUUID().toString(), )
type=io.amichne.konditional.core.BooleanDelegate|kind=class|decl=protected class BooleanDelegate<C : Context>( private val default: Boolean, private val configScope: FlagScope<Boolean, C, Namespace>.() -> Unit, )
type=io.amichne.konditional.core.StringDelegate|kind=class|decl=protected class StringDelegate<C : Context>( private val default: String, private val configScope: FlagScope<String, C, Namespace>.() -> Unit, )
type=io.amichne.konditional.core.IntDelegate|kind=class|decl=protected class IntDelegate<C : Context>( private val default: Int, private val configScope: FlagScope<Int, C, Namespace>.() -> Unit, )
type=io.amichne.konditional.core.DoubleDelegate|kind=class|decl=protected class DoubleDelegate<C : Context>( private val default: Double, private val configScope: FlagScope<Double, C, Namespace>.() -> Unit, )
type=io.amichne.konditional.core.EnumDelegate|kind=class|decl=protected class EnumDelegate<E : Enum<E>, C : Context>( private val default: E, private val configScope: FlagScope<E, C, Namespace>.() -> Unit, )
type=io.amichne.konditional.core.KotlinClassDelegate|kind=class|decl=protected class KotlinClassDelegate<T : Konstrained<*>, C : Context>( private val default: T, private val configScope: FlagScope<T, C, Namespace>.() -> Unit, )
fields:
- private val _features
- private lateinit var feature: BooleanFeature<C, *>
- private lateinit var feature: StringFeature<C, *>
- private lateinit var feature: IntFeature<C, *>
- private lateinit var feature: DoubleFeature<C, *>
- private lateinit var feature: EnumFeature<E, C, *>
- private lateinit var feature: KotlinClassFeature<T, C, *>
methods:
- fun allFeatures(): List<Feature<*, *, *>>
- protected fun <C : Context> boolean( default: Boolean, flagScope: FlagScope<Boolean, C, Namespace>.() -> Unit = {}, ): BooleanDelegate<C>
- protected fun <C : Context> string( default: String, stringScope: FlagScope<String, C, Namespace>.() -> Unit = {}, ): StringDelegate<C>
- protected fun <C : Context> integer( default: Int, integerScope: FlagScope<Int, C, Namespace>.() -> Unit = {}, ): IntDelegate<C>
- protected fun <C : Context> double( default: Double, decimalScope: FlagScope<Double, C, Namespace>.() -> Unit = {}, ): DoubleDelegate<C>
- protected fun <E : Enum<E>, C : Context> enum( default: E, enumScope: FlagScope<E, C, Namespace>.() -> Unit = {}, ): EnumDelegate<E, C>
- protected inline fun <reified T : Konstrained<*>, C : Context> custom( default: T, noinline customScope: FlagScope<T, C, Namespace>.() -> Unit = {}, ): KotlinClassDelegate<T, C>
- private inline fun <T : Any, C : Context, M : Namespace, F : Feature<T, C, M>, D> registerFeature( thisRef: M, property: KProperty<*>, default: T, configScope: FlagScope<T, C, Namespace>.() -> Unit, featureFactory: (String, M) -> F, featureSetter: (F) -> Unit, delegateInstance: D, ): D
- internal fun updateDefinitionInternal(definition: FlagDefinition<*, *, *>)
- private fun runtimeRegistry(): NamespaceRegistryRuntime
- override fun equals(other: Any?): Boolean
- override fun hashCode(): Int
- override fun toString(): String
- operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): BooleanDelegate<C>
- operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): BooleanFeature<C, M>
- operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): StringDelegate<C>
- operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): StringFeature<C, M>
- operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): IntDelegate<C>
- operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): IntFeature<C, M>
- operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): DoubleDelegate<C>
- operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): DoubleFeature<C, M>
- operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): EnumDelegate<E, C>
- operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): EnumFeature<E, C, M>
- operator fun <M : Namespace> provideDelegate( thisRef: M, property: KProperty<*>, ): KotlinClassDelegate<T, C>
- operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): KotlinClassFeature<T, C, M>
