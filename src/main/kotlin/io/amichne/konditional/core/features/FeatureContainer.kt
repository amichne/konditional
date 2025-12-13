package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.registry.NamespaceRegistry.Companion.updateDefinition
import io.amichne.konditional.core.types.BooleanEncodeable
import io.amichne.konditional.core.types.DataClassEncodeable
import io.amichne.konditional.core.types.DecimalEncodeable
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.core.types.EnumEncodeable
import io.amichne.konditional.core.types.IntEncodeable
import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.konditional.core.types.StringEncodeable
import io.amichne.konditional.internal.builders.FlagBuilder
import io.amichne.kontracts.schema.ObjectSchema
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base class for organizing and auto-registering feature flags with type-safe delegation.
 *
 * FeatureContainer provides a declarative way to define feature flags using Kotlin's
 * property delegation pattern. Features are automatically registered on first access
 * and can be enumerated at runtime using [allFeatures].
 *
 * **Benefits over enum-based features:**
 * - **Complete enumeration**: `allFeatures()` provides all features at runtime
 * - **Ergonomic delegation**: Use `by boolean {}`, `by string {}`, etc. with DSL configuration
 * - **Single namespace declaration**: No need to repeat namespace on every feature
 * - **Mixed types**: Combine Boolean, String, Int, Double, and Enum features in one container
 * - **Type safety**: Full type inference and compile-time checking
 * - **Lazy registration**: Features are created and registered only when first accessed
 *
 * **Example:**
 * ```kotlin
 * object PaymentFeatures : FeatureContainer<Namespace.Payments>(
 *     Namespace.Payments
 * ) {
 *     val APPLE_PAY by boolean(default = false) {
 *         rule(true) {
 *             platforms(Platform.IOS)
 *         }
 *     }
 *
 *     val CARD_LIMIT by integer(default = 5000) {
 *         rule(10000) {
 *             platforms(Platform.WEB)
 *         }
 *     }
 * }
 *
 * // Complete enumeration
 * val all = PaymentFeatures.allFeatures()
 *
 * // Usage (same as standard API)
 * PaymentFeatures.APPLE_PAY.evaluate(context)
 * ```
 *
 * @param M The namespace type this features belongs to (e.g., Namespace.Payments)
 */
abstract class FeatureContainer<M : Namespace>(
    @PublishedApi
    internal val namespace: M,
) : FeatureAware<M> {
    override val container: FeatureContainer<M>
        get() = this

    private val _features = mutableListOf<Feature<*, *, *, M>>()

    /**
     * Returns all features declared in this features.
     *
     * Features are registered lazily when first accessed through property delegation.
     * This method returns a snapshot of all features that have been accessed at least once.
     *
     * **Note**: Features are only added to this list when their delegated property is
     * accessed for the first time. If you need to ensure all features are registered,
     * access each property at least once (e.g., during initialization).
     *
     * **Example:**
     * ```kotlin
     * val features = PaymentFeatures.allFeatures()
     * features.forEach { feature ->
     *     println("Feature: ${feature.name}")
     * }
     * ```
     *
     * @return An immutable list of all registered features in this features
     */
    fun allFeatures(): List<Feature<*, *, *, M>> = _features.toList()

    /**
     * Creates a Boolean feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * boolean-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
     *     val DARK_MODE by boolean(default = false) {
     *         rule(true) {
     *             platforms(Platform.IOS)
     *         }
     *     }
     * }
     * ```
     *
     * @param C The contextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param flagScope DSL scope for configuring the boolean feature
     * @return A delegated property that returns a [BooleanFeature]
     */
    protected fun <C : Context> boolean(
        default: Boolean,
        flagScope: FlagScope<BooleanEncodeable, Boolean, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<FeatureContainer<M>, BooleanFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default, flagScope) { BooleanFeature(it, namespace) }

    /**
     * Creates a String feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * string-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
     *     val API_ENDPOINT by string(default = "https://api.example.com") {
     *         rule("https://api-android.example.com") {
     *             platforms(Platform.ANDROID)
     *         }
     *     }
     * }
     * ```
     *
     * @param C The contextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param stringScope DSL scope for configuring the string feature
     * @return A delegated property that returns a [StringFeature]
     */
    protected fun <C : Context> string(
        default: String,
        stringScope: FlagScope<StringEncodeable, String, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<FeatureContainer<M>, StringFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default, stringScope) { StringFeature.Companion(it, namespace) }

    /**
     * Creates an Int feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * integer-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
     *     val MAX_RETRY_COUNT by integer(default = 3) {
     *         rule(5) {
     *             platforms(Platform.IOS)
     *         }
     *     }
     * }
     * ```
     *
     * @param C The contextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param integerScope DSL scope for configuring the integer feature
     * @return A delegated property that returns an [IntFeature]
     */
    protected fun <C : Context> integer(
        default: Int,
        integerScope: FlagScope<IntEncodeable, Int, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<FeatureContainer<M>, IntFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default, integerScope) {
            IntFeature.Companion(it, namespace)
        }


    /**
     * Creates a Double feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * decimal-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
     *     val TRANSACTION_FEE by double(default = 0.029) {
     *         rule(0.019) {
     *             platforms(Platform.WEB)
     *         }
     *     }
     * }
     * ```
     *
     * @param C The contextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param decimalScope DSL scope for configuring the double feature
     * @return A delegated property that returns a [DoubleFeature]
     */
    protected fun <C : Context> double(
        default: Double,
        decimalScope: FlagScope<DecimalEncodeable, Double, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<FeatureContainer<M>, DoubleFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default, decimalScope) { DoubleFeature(it, namespace) }

    /**
     * Creates an Enum feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * enum-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * enum class LogLevel { DEBUG, INFO, WARN, ERROR }
     *
     * object MyFeatures : FeatureContainer<Namespace.Logging>(Namespace.Logging) {
     *     val LOG_LEVEL by enum(default = LogLevel.INFO) {
     *         rule(LogLevel.DEBUG) {
     *             dimension(Environment, Environment.DEVELOPMENT)
     *         }
     *     }
     * }
     * ```
     *
     * @param E The enum type
     * @param C The context type used for evaluation
     * @param default The default enum value for this feature (required)
     * @param enumScope DSL scope for configuring the enum feature
     * @return A delegated property that returns an [EnumFeature]
     */
    protected fun <E : Enum<E>, C : Context> enum(
        default: E,
        enumScope: FlagScope<EnumEncodeable<E>, E, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<FeatureContainer<M>, EnumFeature<E, C, M>> =
        ContainerFeaturePropertyDelegate(default, enumScope) { EnumFeature(it, namespace) }

    /**
     * Creates a custom encodeable feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * custom type configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * The custom type must implement [KotlinEncodeable]<[ObjectSchema]>
     * and be annotated with [@ConfigDataClass][io.amichne.konditional.core.dsl.ConfigDataClass]
     * for compile-time schema generation.
     *
     * **Example:**
     * ```kotlin
     * @ConfigDataClass
     * data class PaymentConfig(
     *     val maxRetries: Int = 3,
     *     val timeout: Double = 30.0,
     *     val enabled: Boolean = true
     * ) : KotlinEncodeable<JsonSchema.ObjectSchema> {
     *     override val schema = jsonObject {
     *         field("maxRetries", required = true, default = 3) { int() }
     *         field("timeout", required = true, default = 30.0) { double() }
     *         field("enabled", required = true, default = true) { boolean() }
     *     }
     * }
     *
     * object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
     *     val PAYMENT_CONFIG by custom(default = PaymentConfig()) {
     *         rule(PaymentConfig(maxRetries = 5, timeout = 60.0)) {
     *             dimension(Environment, Environment.PRODUCTION)
     *         }
     *     }
     * }
     * ```
     *
     * @param T The custom type implementing [KotlinEncodeable]<[ObjectSchema]>
     * @param C The context type used for evaluation
     * @param default The default value for this feature (required)
     * @param customScope DSL scope for configuring the custom feature
     * @return A delegated property that returns a [DataClassFeature]
     */
    protected inline fun <reified T : KotlinEncodeable<ObjectSchema>, C : Context> custom(
        default: T,
        noinline customScope: FlagScope<DataClassEncodeable<T>, T, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<FeatureContainer<M>, DataClassFeature<T, C, M>> =
        ContainerFeaturePropertyDelegate(default, customScope) { DataClassFeature(it, namespace) }


    /**
     * Internal delegate factory that handles feature creation, configuration, and registration.
     *
     * This class implements both [ReadOnlyProperty] and [PropertyDelegateProvider] to enable
     * Kotlin's property delegation pattern with lazy initialization. It performs the following:
     *
     * 1. **Captures property name**: When the property is delegated, captures the property name
     *    to use as the feature key
     * 2. **Lazy initialization**: Creates the feature only on first access
     * 3. **Automatic registration**: Adds the feature to the features's feature list
     * 4. **Automatic configuration**: Executes the DSL configuration block and updates the namespace
     *
     * **Implementation details:**
     * - The `factory` function is responsible for creating the feature instance
     * - The feature is created using the property name as the feature key
     * - The DSL configuration block is executed and applied to the namespace
     * - All features in the features share the same namespace
     *
     * @param F The feature type (BooleanFeature, StringFeature, etc.)
     * @param S The EncodableValue type wrapping the actual value
     * @param T The value type (Boolean, String, Int, etc.)
     * @param C The contextFn type used for evaluation
     * @param configScope The DSL configuration block to execute
     * @param factory A function that creates the feature given the namespace and property name
     */
    @Suppress("UNCHECKED_CAST")
    inner class ContainerFeaturePropertyDelegate<F : Feature<S, T, C, M>, S : EncodableValue<T>, T : Any, C : Context>(
        private val default: T,
        private val configScope: FlagScope<S, T, C, M>.() -> Unit,
        private val factory: M.(String) -> F,
    ) : ReadOnlyProperty<FeatureContainer<M>, F>, PropertyDelegateProvider<FeatureContainer<M>, F> {
        lateinit var name: String

        private val feature: F by lazy {
            factory(namespace, name).also { _features.add(it) }.also {
                namespace.updateDefinition(FlagBuilder(default, it).apply<FlagBuilder<S, T, C, M>>(configScope).build())
            }
        }

        override fun getValue(
            thisRef: FeatureContainer<M>,
            property: KProperty<*>,
        ): F = provideDelegate(thisRef, property)

        override fun provideDelegate(
            thisRef: FeatureContainer<M>,
            property: KProperty<*>,
        ): F = run { name = property.name }.let { feature }
    }
}
