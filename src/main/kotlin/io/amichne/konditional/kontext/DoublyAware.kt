package io.amichne.konditional.kontext

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.Namespace.Authentication.flag
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.BooleanFeature
import io.amichne.konditional.core.features.DataClassFeature
import io.amichne.konditional.core.features.DoubleFeature
import io.amichne.konditional.core.features.EnumFeature
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.IntFeature
import io.amichne.konditional.core.features.StringFeature
import io.amichne.konditional.core.registry.NamespaceRegistry.Companion.updateDefinition
import io.amichne.konditional.core.types.DataClassEncodeable
import io.amichne.konditional.core.types.DataClassWithSchema
import io.amichne.konditional.core.types.DecimalEncodeable
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.core.types.EnumEncodeable
import io.amichne.konditional.core.types.IntEncodeable
import io.amichne.konditional.core.types.StringEncodeable
import io.amichne.konditional.internal.builders.FlagBuilder
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DoublyAware<C : Kontext<M>, M : Namespace> : KontextAware<C, M>, FeatureAware<M> {

//    @OptIn(ExperimentalTypeInference::class)
//    @Suppress("UNCHECKED_CAST")
//    inline fun <reified C : Kontext<M>, reified M : Namespace, O> kontextualize(
//        crossinline factory: () -> C,
//    ): O where O : KontextAware<C, M>, O : FeatureContainer<M> = object : KontextAware<C, M>, FeatureContainer<M>() {
//        override fun factory(): C = factory()
//    } as O

    fun <S : EncodableValue<T>, T : Any> feature(
        block: DoublyAware<C, M>.() -> Feature<S, T, C, M>,
    ): T = flag(block()).evaluate(kontext)


    fun <S : EncodableValue<T>, T : Any> feature(
        @KonditionalDsl kontext: () -> C,
        block: KontextAware<C, M>.() -> Feature<S, T, C, M>,
    ): T = flag(block()).evaluate(kontext())

    fun <S : EncodableValue<T>, T : Any> feature(
        @KonditionalDsl kontext: C,
        block: KontextAware<C, M>.() -> Feature<S, T, C, M>,
    ): T = flag(block()).evaluate(kontext)


    fun <C : Kontext<M>> boolean(
        default: Boolean,
        flagScope: FlagScope<EncodableValue<Boolean>, Boolean, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<DoublyAware<C, M>, Feature<EncodableValue<Boolean>, Boolean, C, M>> =
        ContainerFeaturePropertyDelegate(namespace, default, flagScope) { BooleanFeature.Companion(it, namespace) }.let {
            it.provideDelegate(this, this::)
        }

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
     *         rule {
     *             platforms(Platform.ANDROID)
     *         } returns "https://api-android.example.com"
     *     }
     * }
     * ```
     *
     * @param C The kontextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param stringScope DSL scope for configuring the string feature
     * @return A delegated property that returns a [io.amichne.konditional.core.features.StringFeature]
     */
    fun <C : Kontext<M>> string(
        default: String,
        stringScope: FlagScope<StringEncodeable, String, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<DoublyAware<C, M>, StringFeature<C, M>> =
        ContainerFeaturePropertyDelegate(namespace, default, stringScope) { StringFeature.Companion(it, namespace) }

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
     *     val MAX_RETRY_COUNT by int(default = 3) {
     *         rule {
     *             platforms(Platform.IOS)
     *         } returns 5
     *     }
     * }
     * ```
     *
     * @param C The kontextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param integerScope DSL scope for configuring the integer feature
     * @return A delegated property that returns an [io.amichne.konditional.core.features.IntFeature]
     */
    fun <C : Kontext<M>> integer(
        default: Int,
        integerScope: FlagScope<IntEncodeable, Int, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<DoublyAware<C, M>, IntFeature<C, M>> =
        ContainerFeaturePropertyDelegate(namespace, default, integerScope) { IntFeature.Companion(it, namespace) }

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
     *         rule {
     *             platforms(Platform.IOS)
     *         } returns 0.019
     *     }
     * }
     * ```
     *
     * @param C The kontextFn type used for evaluation
     * @param default The default value for this feature (required)
     * @param decimalScope DSL scope for configuring the double feature
     * @return A delegated property that returns a [DoubleFeature]
     */
    fun <C : Kontext<M>> double(
        default: Double,
        decimalScope: FlagScope<DecimalEncodeable, Double, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<DoublyAware<C, M>, DoubleFeature<C, M>> =
        ContainerFeaturePropertyDelegate(namespace, default, decimalScope) { DoubleFeature(it, namespace) }

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
     *         rule {
     *             environments(Environment.DEVELOPMENT)
     *         } returns LogLevel.DEBUG
     *     }
     * }
     * ```
     *
     * @param E The enum type
     * @param C The kontext type used for evaluation
     * @param default The default enum value for this feature (required)
     * @param enumScope DSL scope for configuring the enum feature
     * @return A delegated property that returns an [EnumFeature]
     */
    fun <E : Enum<E>, C : Kontext<M>> enum(
        default: E,
        enumScope: FlagScope<EnumEncodeable<E>, E, C, M>.() -> Unit =
            {},
    ): ReadOnlyProperty<DoublyAware<C, M>, EnumFeature<E, C, M>> =
        ContainerFeaturePropertyDelegate(namespace, default, enumScope) { EnumFeature(it, namespace) }

    /**
     * Creates a DataClass feature with automatic registration and configuration.
     *
     * The feature is configured using a DSL scope that provides type-safe access to
     * data class-specific configuration options like rules, defaults, and targeting.
     * Configuration is automatically applied to the namespace when the feature is first accessed.
     *
     * The data class must implement [DataClassWithSchema] and be annotated with [@ConfigDataClass][io.amichne.konditional.core.dsl.ConfigDataClass]
     * for compile-time schema generation.
     *
     * **Example:**
     * ```kotlin
     * @ConfigDataClass
     * data class PaymentConfig(
     *     val maxRetries: Int = 3,
     *     val timeout: Double = 30.0,
     *     val enabled: Boolean = true
     * ) : DataClassWithSchema
     *
     * object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
     *     val PAYMENT_CONFIG by dataClass(default = PaymentConfig()) {
     *         rule {
     *             environments(Environment.PRODUCTION)
     *         } returns PaymentConfig(maxRetries = 5, timeout = 60.0)
     *     }
     * }
     * ```
     *
     * @param T The data class type implementing DataClassWithSchema
     * @param C The kontext type used for evaluation
     * @param default The default value for this feature (required)
     * @param dataClassScope DSL scope for configuring the data class feature
     * @return A delegated property that returns a [DataClassFeature]
     */
    fun <T : DataClassWithSchema, C : Kontext<M>> dataClass(
        default: T,
        dataClassScope: FlagScope<DataClassEncodeable<T>, T, C, M>.() -> Unit = {},
    ): ReadOnlyProperty<DoublyAware<C, M>, DataClassFeature<T, C, M>> =
        ContainerFeaturePropertyDelegate(namespace, default, dataClassScope)
        { DataClassFeature(it, namespace) }

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
     * @param C The kontextFn type used for evaluation
     * @param configScope The DSL configuration block to execute
     * @param factory A function that creates the feature given the namespace and property name
     */
    @Suppress("UNCHECKED_CAST")
    class ContainerFeaturePropertyDelegate<F : Feature<S, T, C, M>, S : EncodableValue<T>, T : Any, C : Kontext<M>, M : Namespace>(
        private val namespace: M,
        private val default: T,
        private val configScope: FlagScope<S, T, C, M>.() -> Unit,
        private val factory: M.(String) -> F,
    ) : ReadOnlyProperty<DoublyAware<C, M>, F>, PropertyDelegateProvider<DoublyAware<C, M>, F> {
        lateinit var name: String

        private val feature: F by lazy {
            factory(namespace, name).also { createdFeature ->
                // Execute the DSL configuration block and update the namespace
                val flagDefinition = FlagBuilder(createdFeature).apply(configScope).apply { default(default) }.build()
                namespace.updateDefinition(flagDefinition)
            }
        }

        override fun getValue(
            thisRef: DoublyAware<C, M>,
            property: KProperty<*>,
        ): F = provideDelegate(thisRef, property)

        override fun provideDelegate(
            thisRef: DoublyAware<C, M>,
            property: KProperty<*>,
        ): F = run { name = property.name }.let { feature }
    }
}
