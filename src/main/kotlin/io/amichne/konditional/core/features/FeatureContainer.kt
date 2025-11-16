package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.ModuleRegistry
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.builders.FlagBuilder
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
 * - **Single taxonomy declaration**: No need to repeat taxonomy on every feature
 * - **Mixed types**: Combine Boolean, String, Int, Double, JSON, and custom features in one container
 * - **Type safety**: Full type inference and compile-time checking
 * - **Lazy registration**: Features are created and registered only when first accessed
 *
 * **Example:**
 * ```kotlin
 * object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
 *     Taxonomy.Domain.Payments
 * ) {
 *     val APPLE_PAY by boolean {
 *         default(false)
 *         rule {
 *             platforms(Platform.IOS)
 *         } implies true
 *     }
 *
 *     val CARD_LIMIT by int {
 *         default(5000)
 *         rule {
 *             platforms(Platform.WEB)
 *         } implies 10000
 *     }
 *
 *     val API_CONFIG by jsonObject<ApiConfig>(
 *         default = ApiConfig("https://api.example.com", 30),
 *         key = "api_config"
 *     )
 * }
 *
 * // Complete enumeration
 * val all = PaymentFeatures.allFeatures()
 *
 * // Usage (same as standard API)
 * context.evaluateSafe(PaymentFeatures.APPLE_PAY)
 * ```
 *
 * @param M The taxonomy type this container belongs to (e.g., Taxonomy.Domain.Payments)
 */
abstract class FeatureContainer<M : Taxonomy>(
    @PublishedApi
    internal val taxonomy: M,
) {
    private val _features = mutableListOf<Feature<*, *, *, M>>()

    /**
     * Returns all features declared in this container.
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
     * @return An immutable list of all registered features in this container
     */
    fun allFeatures(): List<Feature<*, *, *, M>> = _features.toList()

    /**
     * Creates a Boolean feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * boolean-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the registry when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
     *     val DARK_MODE by boolean {
     *         default(false)
     *         rule {
     *             platforms(Platform.IOS)
     *         } implies true
     *     }
     * }
     * ```
     *
     * @param C The context type used for evaluation
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param flagScope DSL scope for configuring the boolean feature
     * @return A delegated property that returns a [BooleanFeature]
     */
    protected fun <C : Context> boolean(
        registry: ModuleRegistry = taxonomy.registry,
        flagScope: FlagScope<EncodableValue.BooleanEncodeable, Boolean, C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, BooleanFeature<C, M>> =
        ContainerFeaturePropertyDelegate(registry, flagScope) { BooleanFeature(it, taxonomy) }

    /**
     * Creates a String feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * string-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the registry when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
     *     val API_ENDPOINT by string {
     *         default("https://api.example.com")
     *         rule {
     *             platforms(Platform.ANDROID)
     *         } implies "https://api-android.example.com"
     *     }
     * }
     * ```
     *
     * @param C The context type used for evaluation
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param stringScope DSL scope for configuring the string feature
     * @return A delegated property that returns a [StringFeature]
     */
    protected fun <C : Context> string(
        registry: ModuleRegistry = taxonomy.registry,
        stringScope: FlagScope<EncodableValue.StringEncodeable, String, C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, StringFeature<C, M>> =
        ContainerFeaturePropertyDelegate(registry, stringScope) { StringFeature.Companion(it, taxonomy) }

    /**
     * Creates an Int feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * integer-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the registry when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
     *     val MAX_RETRY_COUNT by int {
     *         default(3)
     *         rule {
     *             platforms(Platform.IOS)
     *         } implies 5
     *     }
     * }
     * ```
     *
     * @param C The context type used for evaluation
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param integerScope DSL scope for configuring the integer feature
     * @return A delegated property that returns an [IntFeature]
     */
    protected fun <C : Context> int(
        registry: ModuleRegistry = taxonomy.registry,
        integerScope: FlagScope<EncodableValue.IntEncodeable, Int, C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, IntFeature<C, M>> =
        ContainerFeaturePropertyDelegate(registry, integerScope) {
            IntFeature.Companion(it, taxonomy, registry)
        }

    /**
     * Creates a Double feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * decimal-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the registry when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * object MyFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
     *     val TRANSACTION_FEE by double {
     *         default(0.029)
     *         rule {
     *             platforms(Platform.WEB)
     *         } implies 0.019
     *     }
     * }
     * ```
     *
     * @param C The context type used for evaluation
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param decimalScope DSL scope for configuring the double feature
     * @return A delegated property that returns a [DoubleFeature]
     */
    protected fun <C : Context> double(
        registry: ModuleRegistry = taxonomy.registry,
        decimalScope: FlagScope<EncodableValue.DecimalEncodeable, Double, C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, DoubleFeature<C, M>> =
        ContainerFeaturePropertyDelegate(registry, decimalScope) { DoubleFeature(it, taxonomy, registry) }

    /**
     * Creates a JSON object feature with automatic registration and configuration.
     *
     * This method is used for complex data structures that need to be serialized as JSON.
     * The type T must be a data class or other JSON-serializable type.
     * Configuration is automatically applied to the registry when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * data class ApiConfig(val baseUrl: String, val timeout: Int)
     *
     * object MyFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
     *     val API_CONFIG by jsonObject(
     *         default = ApiConfig("https://api.example.com", 30),
     *         key = "api_config"
     *     )
     * }
     * ```
     *
     * @param T The data class type for the JSON object
     * @param C The context type used for evaluation
     * @param default The default value when no rules match
     * @param key The unique feature key used for identification
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @return A delegated property that returns a [JsonFeature]
     */
    protected inline fun <C : Context, reified T : Any> jsonObject(
        default: T,
        key: String,
        registry: ModuleRegistry = taxonomy.registry,
    ): ReadOnlyProperty<FeatureContainer<M>, JsonFeature<C, M, T>> =
        ContainerFeaturePropertyDelegate(registry, { default(default) }) { JsonFeature(key, taxonomy, registry) }

    /**
     * Creates a custom wrapper type feature with automatic registration and configuration.
     *
     * This method is used for custom types that need special encoding/decoding logic,
     * such as DateTime, UUID, or other domain-specific wrapper types.
     * Configuration is automatically applied to the registry when the feature is first accessed.
     *
     * **Example:**
     * ```kotlin
     * // Assuming you have a custom DateTime wrapper type
     * object MyFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
     *     val MAINTENANCE_WINDOW by custom<DateTime, String, Context>(
     *         default = DateTime.parse("2024-01-01T00:00:00Z"),
     *         key = "maintenance_window"
     *     )
     * }
     * ```
     *
     * @param T The wrapped type (e.g., LocalDateTime, UUID, custom domain types)
     * @param P The primitive type used for encoding (String, Int, etc.) that extends EncodableValue
     * @param C The context type used for evaluation
     * @param default The default value when no rules match
     * @param key The unique feature key used for identification
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @return A delegated property that returns a [CustomFeature]
     */
    protected inline fun <reified T : Any, reified P, C : Context> custom(
        default: T,
        key: String,
        registry: ModuleRegistry = taxonomy.registry,
    ): ReadOnlyProperty<FeatureContainer<M>, CustomFeature<T, P, C, M>> where
        P : Any, P : EncodableValue<P> = ContainerFeaturePropertyDelegate(registry, { default(default) }) {
        Feature.custom(key, taxonomy, registry)
    }

    /**
     * Internal delegate factory that handles feature creation, configuration, and registration.
     *
     * This class implements both [ReadOnlyProperty] and [PropertyDelegateProvider] to enable
     * Kotlin's property delegation pattern with lazy initialization. It performs the following:
     *
     * 1. **Captures property name**: When the property is delegated, captures the property name
     *    to use as the feature key
     * 2. **Lazy initialization**: Creates the feature only on first access
     * 3. **Automatic registration**: Adds the feature to the container's feature list
     * 4. **Automatic configuration**: Executes the DSL configuration block and updates the registry
     *
     * **Implementation details:**
     * - The `factory` function is responsible for creating the feature instance
     * - The feature is created using the property name as the feature key
     * - The DSL configuration block is executed and applied to the feature's registry
     * - All features in the container share the same taxonomy
     *
     * @param F The feature type (BooleanFeature, StringFeature, etc.)
     * @param S The EncodableValue type wrapping the actual value
     * @param T The value type (Boolean, String, Int, etc.)
     * @param C The context type used for evaluation
     * @param registry The module registry for storing configuration
     * @param configScope The DSL configuration block to execute
     * @param factory A function that creates the feature given the taxonomy and property name
     */
    @Suppress("UNCHECKED_CAST")
    inner class ContainerFeaturePropertyDelegate<F : Feature<S, T, C, M>, S : EncodableValue< T>, T : Any, C : Context>(
        private val registry: ModuleRegistry,
        private val configScope: FlagScope<S, T, C, M>.() -> Unit,
        private val factory: M.(String) -> F,
    ) : ReadOnlyProperty<FeatureContainer<M>, F>, PropertyDelegateProvider<FeatureContainer<M>, F> {
        lateinit var name: String

        private val feature: F by lazy {
            factory(taxonomy, name).also { _features.add(it) }.also { createdFeature ->
                // Execute the DSL configuration block and update the registry
                val flagDefinition = FlagBuilder(createdFeature).apply(configScope).build()
                registry.update(flagDefinition)
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
