package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.ModuleRegistry
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.dsl.BooleanScope
import io.amichne.konditional.core.dsl.DecimalScope
import io.amichne.konditional.core.dsl.IntegerScope
import io.amichne.konditional.core.dsl.StringScope
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
     * Creates a Boolean feature with automatic registration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * boolean-specific configuration options like rules, defaults, and targeting.
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
     * @param default The default value when no rules match (defaults to false)
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param flagScope DSL scope for configuring the boolean feature
     * @return A delegated property that returns a [BooleanFeature]
     */
    protected fun <C : Context> boolean(
        default: Boolean = false,
        registry: ModuleRegistry = taxonomy.registry,
        flagScope: BooleanScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, BooleanFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default) { BooleanFeature(it, taxonomy) }

    /**
     * Creates a String feature with automatic registration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * string-specific configuration options like rules, defaults, and targeting.
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
     * @param default The default value when no rules match (defaults to empty string)
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param stringScope DSL scope for configuring the string feature
     * @return A delegated property that returns a [StringFeature]
     */
    protected fun <C : Context> string(
        default: String = "",
        registry: ModuleRegistry = taxonomy.registry,
        stringScope: StringScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, StringFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default) { StringFeature.Companion(it, taxonomy) }

    /**
     * Creates an Int feature with automatic registration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * integer-specific configuration options like rules, defaults, and targeting.
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
     * @param default The default value when no rules match (defaults to 0)
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param integerScope DSL scope for configuring the integer feature
     * @return A delegated property that returns an [IntFeature]
     */
    protected fun <C : Context> int(
        default: Int = 0,
        registry: ModuleRegistry = taxonomy.registry,
        integerScope: IntegerScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, IntFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default) {

            IntFeature.Companion(it, taxonomy, registry)
        }

    /**
     * Creates a Double feature with automatic registration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * decimal-specific configuration options like rules, defaults, and targeting.
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
     * @param default The default value when no rules match (defaults to 0.0)
     * @param registry The module registry for storing configuration (defaults to taxonomy.registry)
     * @param decimalScope DSL scope for configuring the double feature
     * @return A delegated property that returns a [DoubleFeature]
     */
    protected fun <C : Context> double(
        default: Double = 0.0,
        registry: ModuleRegistry = taxonomy.registry,
        decimalScope: DecimalScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, DoubleFeature<C, M>> =
        ContainerFeaturePropertyDelegate(default) { DoubleFeature(it, taxonomy, registry) }

    /**
     * Creates a JSON object feature with automatic registration.
     *
     * This method is used for complex data structures that need to be serialized as JSON.
     * The type T must be a data class or other JSON-serializable type.
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
        ContainerFeaturePropertyDelegate(default) { JsonFeature(key, taxonomy, registry) }

    /**
     * Creates a custom wrapper type feature with automatic registration.
     *
     * This method is used for custom types that need special encoding/decoding logic,
     * such as DateTime, UUID, or other domain-specific wrapper types.
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
        P : Any, P : EncodableValue<P> = ContainerFeaturePropertyDelegate(default) {
        Feature.custom(key, taxonomy, registry)
    }

    /**
     * Internal delegate factory that handles feature creation and registration.
     *
     * This class implements both [ReadOnlyProperty] and [PropertyDelegateProvider] to enable
     * Kotlin's property delegation pattern with lazy initialization. It performs the following:
     *
     * 1. **Captures property name**: When the property is delegated, captures the property name
     *    to use as the feature key
     * 2. **Lazy initialization**: Creates the feature only on first access
     * 3. **Automatic registration**: Adds the feature to the container's feature list
     * 4. **Default configuration**: Applies the default value via FlagBuilder
     *
     * **Implementation details:**
     * - The `factory` function is responsible for creating the feature instance
     * - The feature is created using the property name as the feature key
     * - The default value is automatically configured in the feature's registry
     * - All features in the container share the same taxonomy
     *
     * @param F The feature type (BooleanFeature, StringFeature, etc.)
     * @param T The value type (Boolean, String, Int, etc.)
     * @param default The default value to configure for this feature
     * @param factory A function that creates the feature given the taxonomy and property name
     */
    inner class ContainerFeaturePropertyDelegate<F : Feature<*, T, *, M>, T : Any>(
        default: T,
        private val factory: M.(String) -> F,
    ) : ReadOnlyProperty<FeatureContainer<M>, F>, PropertyDelegateProvider<FeatureContainer<M>, F> {
        lateinit var name: String

        private val feature: F by lazy {
            factory(taxonomy, name).also { _features.add(it) }.also {
                it.registry.update(FlagBuilder(it).also { builder -> builder.default(default) }.build())
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
