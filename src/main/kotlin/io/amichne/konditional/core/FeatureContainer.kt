package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.builders.FlagBuilder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base class for organizing and auto-registering feature flags.
 *
 * **Benefits over enum-based features:**
 * - Complete enumeration: `allFeatures()` provides all features at runtime
 * - Ergonomic delegation: Use `by boolean()`, `by string()`, etc.
 * - Single module declaration: No need to `override val module` on every entry
 * - Mixed types: Combine Boolean, String, Int, custom features in one container
 * - Type safety: Full type inference and compile-time checking
 *
 * **Example:**
 * ```kotlin
 * object PaymentFeatures : FeatureContainer<Context, Taxonomy.Domain.Payments>(
 *     Taxonomy.Domain.Payments
 * ) {
 *     val APPLE_PAY by boolean("apple_pay")
 *     val CARD_LIMIT by int("card_limit")
 *     val API_CONFIG by jsonObject<ApiConfig>("api_config")
 * }
 *
 * // Complete enumeration
 * val all = PaymentFeatures.allFeatures() // List<Feature<*, *, Context, Taxonomy.Domain.Payments>>
 *
 * // Usage (same as current API)
 * context.evaluateSafe(PaymentFeatures.APPLE_PAY)
 * ```
 *
 * @param C The context type for evaluation
 * @param M The module this container belongs to
 */
abstract class FeatureContainer<C : Context, M : Taxonomy>(
    protected val taxonomy: M
) {
    private val _features = mutableListOf<Feature<*, *, C, M>>()

    /**
     * Returns all features declared in this container.
     * Features are registered lazily on first access.
     */
    fun allFeatures(): List<Feature<*, *, C, M>> = _features.toList()

    infix fun <S : EncodableValue<T>, T : Any> Feature<S, T, C, M>.defined(build: FlagScope<S, T, C, M>.() -> Unit) {
        FlagBuilder(this).apply(build).build()
    }

    /**
     * Creates a Boolean feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [BooleanFeature]
     */
    protected fun boolean(key: String): ReadOnlyProperty<FeatureContainer<C, M>, BooleanFeature<C, M>> {
        return ContainerFeaturePropertyDelegate { BooleanFeature.invoke(key, taxonomy) }
    }

    protected fun boolean(
        key: String,
        flagScope: FlagScope<EncodableValue.BooleanEncodeable, Boolean, C, M>.() -> Unit
    ): ReadOnlyProperty<FeatureContainer<C, M>, BooleanFeature<C, M>> {
        return ContainerFeaturePropertyDelegate { BooleanFeature.invoke(key, taxonomy) }
    }

    /**
     * Creates a String feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [StringFeature]
     */
    protected fun string(key: String): ReadOnlyProperty<FeatureContainer<C, M>, StringFeature<C, M>> {
        return ContainerFeaturePropertyDelegate { StringFeature(key, taxonomy) }
    }

    /**
     * Creates an Int feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns an [IntFeature]
     */
    protected fun int(key: String): ReadOnlyProperty<FeatureContainer<C, M>, IntFeature<C, M>> {
        return ContainerFeaturePropertyDelegate { IntFeature(key, taxonomy) }
    }

    /**
     * Creates a Double feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [DoubleFeature]
     */
    protected fun double(key: String): ReadOnlyProperty<FeatureContainer<C, M>, DoubleFeature<C, M>> {
        return ContainerFeaturePropertyDelegate { DoubleFeature(key, taxonomy) }
    }

    /**
     * Creates a JSON object feature with automatic registration.
     *
     * @param T The data class type
     * @param key The unique feature key
     * @return A delegated property that returns a [Feature.OfJsonObject]
     */
    protected inline fun <reified T : Any> jsonObject(
        key: String
    ): ReadOnlyProperty<FeatureContainer<C, M>, Feature.OfJsonObject<T, C, M>> {
        return ContainerFeaturePropertyDelegate { Feature.jsonObject(key, taxonomy) }
    }

    /**
     * Creates a custom wrapper type feature with automatic registration.
     *
     * @param T The wrapped type (e.g., LocalDateTime, UUID)
     * @param P The primitive type used for encoding (String, Int, etc.)
     * @param key The unique feature key
     * @return A delegated property that returns a [Feature.OfCustom]
     */
    protected inline fun <reified T : Any, reified P> custom(
        key: String
    ): ReadOnlyProperty<FeatureContainer<C, M>, Feature.OfCustom<T, P, C, M>> where
        P : Any, P : EncodableValue<P> {
        return ContainerFeaturePropertyDelegate { Feature.custom(key, taxonomy) }
    }

    /**
     * Internal delegate factory that handles feature creation and registration.
     *
     * Features are created lazily on first property access and automatically
     * registered in the container's feature list.
     */
    inner class ContainerFeaturePropertyDelegate<F : Feature<*, *, C, M>>(private val factory: () -> F) :
        ReadOnlyProperty<FeatureContainer<C, M>, F> {
        private val feature by lazy { factory().also { _features.add(it) } }

        override fun getValue(thisRef: FeatureContainer<C, M>, property: KProperty<*>): F = feature
    }
}
