@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.features.BooleanFeature
import io.amichne.konditional.core.features.DoubleFeature
import io.amichne.konditional.core.features.EnumFeature
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.IntFeature
import io.amichne.konditional.core.features.KotlinClassFeature
import io.amichne.konditional.core.features.StringFeature
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceRegistryFactories
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.core.spi.FeatureRegistrationHooks
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.internal.builders.FlagBuilder
import io.amichne.konditional.values.IdentifierEncoding.SEPARATOR
import org.jetbrains.annotations.TestOnly
import java.util.UUID
import kotlin.reflect.KProperty

/**
 * Represents a feature flag namespace with isolated configuration and runtime isolation.
 *
 * Namespaces provide:
 * - **Compile-time isolation**: Features are type-bound to their namespace
 * - **Runtime isolation**: Each namespace has its own flag registry
 * - **Type safety**: Namespace identity is encoded in the type system
 * - **Direct registry operations**: Namespaces implement [NamespaceRegistry], eliminating the need for `.registry` access
 * - **Inline feature definition**: Define feature flags directly on the namespace via property delegation
 *
 * ## Namespace Types
 *
 * ### Consumer-defined namespaces
 * Define namespaces in your own codebase.
 * A namespace is just a [Namespace] instance, typically modeled as an `object`:
 * ```kotlin
 * object Payments : Namespace("payments")
 * ```
 *
 * ## Adding New Modules
 *
 * Define a namespace in your module, and define flags directly on it:
 *
 * ```kotlin
 * object Payments : Namespace("payments") {
 *     val APPLE_PAY by boolean<Context>(default = false)
 * }
 * ```
 *
 * ## Direct Registry Operations
 *
 * [Namespace] implements [NamespaceRegistry] via delegation, so you can call registry methods directly:
 * ```kotlin
 * // Load configuration
 * Payments.load(configuration)
 *
 * // Get current state
 * val snapshot = Payments.configuration
 *
 * // Query flags
 * val flag = Payments.flag(MY_FLAG)
 * ```
 *
 * @property id Unique identifier for this namespace
 */
open class Namespace(
    val id: String,
    @property:KonditionalInternalApi
    val registry: NamespaceRegistry = NamespaceRegistryFactories.default(id),
    /**
     * Seed used to construct stable [io.amichne.konditional.values.FeatureId] values for features.
     *
     * By default this is the namespace [id], which is appropriate for "real" namespaces that are intended
     * to be federated and stable within an application.
     *
     * Test-only/ephemeral namespaces should provide a per-instance unique seed to avoid collisions.
     */
    @PublishedApi internal val identifierSeed: String = id,
) : NamespaceRegistry by registry {

    init {
        require(id.isNotBlank()) { "Namespace id must not be blank" }
        require(!id.contains(SEPARATOR)) { "Namespace id must not contain '$SEPARATOR': '$id'" }
        require(identifierSeed.isNotBlank()) { "Namespace identifierSeed must not be blank" }
        require(!identifierSeed.contains(SEPARATOR)) {
            "Namespace identifierSeed must not contain '$SEPARATOR': '$identifierSeed'"
        }
    }

    /**
     * Consumer-defined namespaces.
     *
     * Consumers define namespaces in their own codebase by extending [Namespace].
     *
     * Example:
     * ```kotlin
     * object Payments : Namespace("payments")
     * object Auth : Namespace("auth")
     * ```
     */
    @TestOnly
    abstract class TestNamespaceFacade(id: String) : Namespace(
        id = id,
        registry = NamespaceRegistryFactories.default(id),
        identifierSeed = UUID.randomUUID().toString(),
    )

    private val _features = mutableListOf<Feature<*, *, *>>()

    fun allFeatures(): List<Feature<*, *, *>> = _features.toList()

    /**
     * Defines a boolean feature on this namespace using property delegation.
     *
     * The property type and rule values are enforced at compile time. At runtime, a loaded
     * configuration (after validation) can change the effective value, with [default] as the
     * fallback when no rule matches or no configuration is loaded.
     *
     * Example:
     * ```kotlin
     * object Payments : Namespace("payments") {
     *     val applePayEnabled by boolean<Context>(default = false) {
     *         rule(true) { platforms(Platform.IOS) }
     *     }
     * }
     * ```
     *
     * @param default Value used when no rule matches or configuration is absent.
     * @param flagScope DSL for rules, rollout, and targeting criteria.
     */
    protected fun <C : Context> boolean(
        default: Boolean,
        flagScope: FlagScope<Boolean, C, Namespace>.() -> Unit = {},
    ): BooleanDelegate<C> = BooleanDelegate(default, flagScope)

    /**
     * Defines a string feature on this namespace using property delegation.
     *
     * The property type and rule values are enforced at compile time. Runtime configuration is
     * validated at load time; invalid updates are rejected and [default] remains effective.
     *
     * Example:
     * ```kotlin
     * object Checkout : Namespace("checkout") {
     *     val bannerText by string<Context>(default = "Welcome") {
     *         rule("Enterprise") { axis(Axes.Tenant, Tenant.ENTERPRISE) }
     *     }
     * }
     * ```
     *
     * @param default Value used when no rule matches or configuration is absent.
     * @param stringScope DSL for rules, rollout, and targeting criteria.
     */
    protected fun <C : Context> string(
        default: String,
        stringScope: FlagScope<String, C, Namespace>.() -> Unit = {},
    ): StringDelegate<C> = StringDelegate(default, stringScope)

    /**
     * Defines an integer feature on this namespace using property delegation.
     *
     * The property type and rule values are enforced at compile time. Runtime configuration can
     * override the value after validation; [default] remains the fallback when no rule matches.
     *
     * Example:
     * ```kotlin
     * object Pricing : Namespace("pricing") {
     *     val retryLimit by integer<Context>(default = 3)
     * }
     * ```
     *
     * @param default Value used when no rule matches or configuration is absent.
     * @param integerScope DSL for rules, rollout, and targeting criteria.
     */
    protected fun <C : Context> integer(
        default: Int,
        integerScope: FlagScope<Int, C, Namespace>.() -> Unit = {},
    ): IntDelegate<C> = IntDelegate(default, integerScope)

    /**
     * Defines a double feature on this namespace using property delegation.
     *
     * The property type and rule values are enforced at compile time. Runtime configuration is
     * validated at load time; invalid values do not modify the active configuration.
     *
     * Example:
     * ```kotlin
     * object Experiments : Namespace("experiments") {
     *     val discountMultiplier by double<Context>(default = 1.0) {
     *         rule(0.9) { versions { min(2, 0, 0) } }
     *     }
     * }
     * ```
     *
     * @param default Value used when no rule matches or configuration is absent.
     * @param decimalScope DSL for rules, rollout, and targeting criteria.
     */
    protected fun <C : Context> double(
        default: Double,
        decimalScope: FlagScope<Double, C, Namespace>.() -> Unit = {},
    ): DoubleDelegate<C> = DoubleDelegate(default, decimalScope)

    /**
     * Defines an enum feature on this namespace using property delegation.
     *
     * The property type and rule values are enforced at compile time. Runtime configuration is
     * validated against the enum values; unknown values are rejected during parsing.
     *
     * Example:
     * ```kotlin
     * enum class Theme { LIGHT, DARK }
     *
     * object Ui : Namespace("ui") {
     *     val theme by enum<Theme, Context>(default = Theme.LIGHT)
     * }
     * ```
     *
     * @param default Value used when no rule matches or configuration is absent.
     * @param enumScope DSL for rules, rollout, and targeting criteria.
     */
    protected fun <E : Enum<E>, C : Context> enum(
        default: E,
        enumScope: FlagScope<E, C, Namespace>.() -> Unit = {},
    ): EnumDelegate<E, C> = EnumDelegate(default, enumScope)

    /**
     * Defines a custom feature type on this namespace using property delegation.
     *
     * The property type and rule values are enforced at compile time. Runtime configuration is
     * validated against [Konstrained] schemas; invalid payloads are rejected and do not update
     * the active configuration.
     *
     * Example:
     * ```kotlin
     * data class UiConfig(
     *     val variant: String,
     * ) : Konstrained<UiConfigSchema>
     *
     * object Ui : Namespace("ui") {
     *     val config by custom<UiConfig, Context>(default = UiConfig("control"))
     * }
     * ```
     *
     * @param default Value used when no rule matches or configuration is absent.
     * @param customScope DSL for rules, rollout, and targeting criteria.
     */
    protected inline fun <reified T : Konstrained<*>, C : Context> custom(
        default: T,
        noinline customScope: FlagScope<T, C, Namespace>.() -> Unit = {},
    ): KotlinClassDelegate<T, C> = KotlinClassDelegate(default, customScope)

    @Suppress("LongParameterList")
    private inline fun <T : Any, C : Context, M : Namespace, F : Feature<T, C, M>, D> registerFeature(
        thisRef: M,
        property: KProperty<*>,
        default: T,
        configScope: FlagScope<T, C, Namespace>.() -> Unit,
        featureFactory: (String, M) -> F,
        featureSetter: (F) -> Unit,
        delegateInstance: D,
    ): D = delegateInstance.apply {
        featureFactory(property.name, thisRef).also {
            featureSetter(it)
            (thisRef as Namespace)._features.add(it)
            thisRef.updateDefinitionInternal(FlagBuilder(default, it).apply(configScope).build())
            FeatureRegistrationHooks.notifyFeatureDefined(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected class BooleanDelegate<C : Context>(
        private val default: Boolean,
        private val configScope: FlagScope<Boolean, C, Namespace>.() -> Unit,
    ) {
        private lateinit var feature: BooleanFeature<C, *>

        operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): BooleanDelegate<C> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, ns -> BooleanFeature(name, ns) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): BooleanFeature<C, M> =
            feature as BooleanFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class StringDelegate<C : Context>(
        private val default: String,
        private val configScope: FlagScope<String, C, Namespace>.() -> Unit,
    ) {
        private lateinit var feature: StringFeature<C, *>

        operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): StringDelegate<C> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, ns -> StringFeature(name, ns) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): StringFeature<C, M> =
            feature as StringFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class IntDelegate<C : Context>(
        private val default: Int,
        private val configScope: FlagScope<Int, C, Namespace>.() -> Unit,
    ) {
        private lateinit var feature: IntFeature<C, *>

        operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): IntDelegate<C> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, ns -> IntFeature(name, ns) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): IntFeature<C, M> =
            feature as IntFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class DoubleDelegate<C : Context>(
        private val default: Double,
        private val configScope: FlagScope<Double, C, Namespace>.() -> Unit,
    ) {
        private lateinit var feature: DoubleFeature<C, *>

        operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): DoubleDelegate<C> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, ns -> DoubleFeature(name, ns) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): DoubleFeature<C, M> =
            feature as DoubleFeature<C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class EnumDelegate<E : Enum<E>, C : Context>(
        private val default: E,
        private val configScope: FlagScope<E, C, Namespace>.() -> Unit,
    ) {
        private lateinit var feature: EnumFeature<E, C, *>

        operator fun <M : Namespace> provideDelegate(thisRef: M, property: KProperty<*>): EnumDelegate<E, C> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, ns -> EnumFeature(name, ns) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): EnumFeature<E, C, M> =
            feature as EnumFeature<E, C, M>
    }

    @Suppress("UNCHECKED_CAST")
    protected class KotlinClassDelegate<T : Konstrained<*>, C : Context>(
        private val default: T,
        private val configScope: FlagScope<T, C, Namespace>.() -> Unit,
    ) {
        private lateinit var feature: KotlinClassFeature<T, C, *>

        operator fun <M : Namespace> provideDelegate(
            thisRef: M,
            property: KProperty<*>,
        ): KotlinClassDelegate<T, C> =
            thisRef.registerFeature(
                thisRef = thisRef,
                property = property,
                default = default,
                configScope = configScope,
                featureFactory = { name, ns -> KotlinClassFeature(name, ns) },
                featureSetter = { feature = it },
                delegateInstance = this,
            )

        operator fun <M : Namespace> getValue(thisRef: M, property: KProperty<*>): KotlinClassFeature<T, C, M> =
            feature as KotlinClassFeature<T, C, M>
    }

    @OptIn(KonditionalInternalApi::class)
    @PublishedApi
    internal fun updateDefinitionInternal(definition: FlagDefinition<*, *, *>) {
        runtimeRegistry().updateDefinition(definition)
    }

    @OptIn(KonditionalInternalApi::class)
    private fun runtimeRegistry(): NamespaceRegistryRuntime =
        registry as? NamespaceRegistryRuntime
            ?: error(
                "NamespaceRegistryRuntime is required. " +
                    "Add :konditional-runtime to your dependencies to enable runtime operations.",
            )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Namespace) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Namespace($id)"
}
