@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.features.BooleanFeature
import io.amichne.konditional.core.features.DoubleFeature
import io.amichne.konditional.core.features.EnumFeature
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.IntFeature
import io.amichne.konditional.core.features.KotlinClassFeature
import io.amichne.konditional.core.features.StringFeature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.definitionFor
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.spi.FeatureRegistrationHooks
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.internal.builders.FlagBuilder
import io.amichne.konditional.values.NamespaceId
import java.util.UUID
import kotlin.reflect.KProperty

/**
 * Namespace-scoped feature container with atomic runtime state.
 *
 * [facts] is the one vocabulary this namespace's rules can read. Flag declarations receive it as
 * the receiver of every condition, and snapshots resolve their fact identifiers against it.
 * A namespace whose rules read no facts uses [io.amichne.konditional.context.fact.NoFacts].
 */
open class Namespace<S : FactSchema> private constructor(
    val id: NamespaceId,
    val facts: S,
    @property:KonditionalInternalApi
    val registry: NamespaceRegistry = InMemoryNamespaceRegistry(namespaceId = id.value),
    @PublishedApi internal val identifierSeed: NamespaceId.Seed = id.seed(),
) : NamespaceRegistry by registry {
    /** Infers the namespace identifier from the declaring class name. */
    constructor(facts: S) : this(defaultNamespaceId(), facts)

    constructor(id: String, facts: S) : this(NamespaceId(id), facts)

    private val features = mutableListOf<Feature<*, *, *>>()
    private val declaredDefaults = linkedMapOf<Feature<*, *, *>, Any>()
    private val declaredDefinitions = linkedMapOf<Feature<*, *, *>, FlagDefinition<*, *, *>>()

    fun allFeatures(): List<Feature<*, *, *>> = features.toList()

    @KonditionalInternalApi
    fun declaredDefault(feature: Feature<*, *, *>): Any? = declaredDefaults[feature]

    @KonditionalInternalApi
    fun <T : Any, C : Context, M : Namespace<*>> declaredDefinition(
        feature: Feature<T, C, M>,
    ): FlagDefinition<T, C, M>? =
        declaredDefinitions.definitionFor(feature)

    override val configuration: Configuration
        get() {
            val installed = currentSnapshot.configuration
            return installed.copy(flags = declaredDefinitions + installed.flags)
        }

    override fun <T : Any, C : Context, M : Namespace<*>> flag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> =
        findFlag(key)
            ?: error("Flag not found for feature '${key.key}' in namespace '$id'.")

    override fun <T : Any, C : Context, M : Namespace<*>> findFlag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M>? =
        registry.findFlag(key)
            ?: declaredDefinition(key)

    protected fun <C : Context> boolean(
        default: Boolean,
        flagScope: FlagScope<Boolean, C, S>.() -> Unit = {},
    ): BooleanDelegate<C, S> = BooleanDelegate(default, flagScope)

    protected fun <C : Context> string(
        default: String,
        stringScope: FlagScope<String, C, S>.() -> Unit = {},
    ): StringDelegate<C, S> = StringDelegate(default, stringScope)

    protected fun <C : Context> integer(
        default: Int,
        integerScope: FlagScope<Int, C, S>.() -> Unit = {},
    ): IntDelegate<C, S> = IntDelegate(default, integerScope)

    protected fun <C : Context> double(
        default: Double,
        decimalScope: FlagScope<Double, C, S>.() -> Unit = {},
    ): DoubleDelegate<C, S> = DoubleDelegate(default, decimalScope)

    protected fun <E : Enum<E>, C : Context> enum(
        default: E,
        enumScope: FlagScope<E, C, S>.() -> Unit = {},
    ): EnumDelegate<E, C, S> = EnumDelegate(default, enumScope)

    protected inline fun <reified T : Konstrained, C : Context> custom(
        default: T,
        noinline customScope: FlagScope<T, C, S>.() -> Unit = {},
    ): KotlinClassDelegate<T, C, S> = KotlinClassDelegate(default, customScope)

    @Suppress("LongParameterList")
    private inline fun <T : Any, C : Context, M : Namespace<S>, F : Feature<T, C, M>, D> registerFeature(
        thisRef: M,
        property: KProperty<*>,
        default: T,
        configScope: FlagScope<T, C, S>.() -> Unit,
        featureFactory: (String, M) -> F,
        featureSetter: (F) -> Unit,
        delegateInstance: D,
    ): D =
        delegateInstance.apply {
            featureFactory(property.name, thisRef).also { feature ->
                val definition = FlagBuilder(default, feature).apply(configScope).build()
                featureSetter(feature)
                thisRef.features += feature
                thisRef.declaredDefaults[feature] = default
                thisRef.declaredDefinitions[feature] = definition
                FeatureRegistrationHooks.notifyFeatureDefined(feature)
            }
        }

    @Suppress("UNCHECKED_CAST")
    protected class BooleanDelegate<C : Context, S : FactSchema>(
        private val default: Boolean,
        private val configScope: FlagScope<Boolean, C, S>.() -> Unit,
    ) {
        private lateinit var feature: BooleanFeature<C, *>

        operator fun <M : Namespace<S>> provideDelegate(thisRef: M, property: KProperty<*>): BooleanDelegate<C, S> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, namespace -> BooleanFeature(name, namespace) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace<S>> getValue(thisRef: M, property: KProperty<*>): BooleanFeature<C, M> =
            feature as BooleanFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class StringDelegate<C : Context, S : FactSchema>(
        private val default: String,
        private val configScope: FlagScope<String, C, S>.() -> Unit,
    ) {
        private lateinit var feature: StringFeature<C, *>

        operator fun <M : Namespace<S>> provideDelegate(thisRef: M, property: KProperty<*>): StringDelegate<C, S> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, namespace -> StringFeature(name, namespace) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace<S>> getValue(thisRef: M, property: KProperty<*>): StringFeature<C, M> =
            feature as StringFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class IntDelegate<C : Context, S : FactSchema>(
        private val default: Int,
        private val configScope: FlagScope<Int, C, S>.() -> Unit,
    ) {
        private lateinit var feature: IntFeature<C, *>

        operator fun <M : Namespace<S>> provideDelegate(thisRef: M, property: KProperty<*>): IntDelegate<C, S> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, namespace -> IntFeature(name, namespace) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace<S>> getValue(thisRef: M, property: KProperty<*>): IntFeature<C, M> =
            feature as IntFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class DoubleDelegate<C : Context, S : FactSchema>(
        private val default: Double,
        private val configScope: FlagScope<Double, C, S>.() -> Unit,
    ) {
        private lateinit var feature: DoubleFeature<C, *>

        operator fun <M : Namespace<S>> provideDelegate(thisRef: M, property: KProperty<*>): DoubleDelegate<C, S> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, namespace -> DoubleFeature(name, namespace) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace<S>> getValue(thisRef: M, property: KProperty<*>): DoubleFeature<C, M> =
            feature as DoubleFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class EnumDelegate<E : Enum<E>, C : Context, S : FactSchema>(
        private val default: E,
        private val configScope: FlagScope<E, C, S>.() -> Unit,
    ) {
        private lateinit var feature: EnumFeature<E, C, *>

        operator fun <M : Namespace<S>> provideDelegate(thisRef: M, property: KProperty<*>): EnumDelegate<E, C, S> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, namespace -> EnumFeature(name, namespace) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace<S>> getValue(thisRef: M, property: KProperty<*>): EnumFeature<E, C, M> =
            feature as EnumFeature<E, C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class KotlinClassDelegate<T : Konstrained, C : Context, S : FactSchema>(
        private val default: T,
        private val configScope: FlagScope<T, C, S>.() -> Unit,
    ) {
        private lateinit var feature: KotlinClassFeature<T, C, *>

        operator fun <M : Namespace<S>> provideDelegate(
            thisRef: M,
            property: KProperty<*>,
        ): KotlinClassDelegate<T, C, S> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, namespace -> KotlinClassFeature(name, namespace) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace<S>> getValue(thisRef: M, property: KProperty<*>): KotlinClassFeature<T, C, M> =
            feature as KotlinClassFeature<T, C, M>
    }

    abstract class TestNamespaceFacade<S : FactSchema>(
        id: String,
        facts: S,
        registry: NamespaceRegistry = InMemoryNamespaceRegistry(namespaceId = id),
        identifierSeed: String = UUID.randomUUID().toString(),
    ) : Namespace<S>(
            id = NamespaceId(id),
            facts = facts,
            registry = registry,
            identifierSeed = NamespaceId.Seed(identifierSeed),
        )

    override fun equals(other: Any?): Boolean =
        other is Namespace<*> && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Namespace($id)"

    private companion object {
        fun defaultNamespaceId(): NamespaceId {
            val inferredClassName =
                Thread.currentThread().stackTrace
                    .asSequence()
                    .map { it.className }
                    .firstOrNull { candidate ->
                        candidate != Namespace::class.java.name &&
                            runCatching {
                                Namespace::class.java.isAssignableFrom(Class.forName(candidate))
                            }.getOrDefault(false)
                    }

            return NamespaceId(
                requireNotNull(inferredClassName) {
                    "Unable to infer namespace id. Provide Namespace(\"<stable-id>\", facts) explicitly."
                },
            )
        }
    }
}
