package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.modules.FeatureModule
import io.amichne.konditional.serialization.EncodableValue
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
 * object PaymentFeatures : FeatureContainer<Context, FeatureModule.Team.Payments>(
 *     FeatureModule.Team.Payments
 * ) {
 *     val APPLE_PAY by boolean("apple_pay")
 *     val CARD_LIMIT by int("card_limit")
 *     val API_CONFIG by jsonObject<ApiConfig>("api_config")
 * }
 *
 * // Complete enumeration
 * val all = PaymentFeatures.allFeatures() // List<Feature<*, *, Context, FeatureModule.Team.Payments>>
 *
 * // Usage (same as current API)
 * context.evaluateSafe(PaymentFeatures.APPLE_PAY)
 * ```
 *
 * @param C The context type for evaluation
 * @param M The module this container belongs to
 */
abstract class FeatureContainer<C : Context, M : FeatureModule>(
    protected val module: M
) {
    private val _features = mutableListOf<Feature<*, *, C, M>>()

    /**
     * Returns all features declared in this container.
     * Features are registered lazily on first access.
     */
    fun allFeatures(): List<Feature<*, *, C, M>> = _features.toList()

    /**
     * Creates a Boolean feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [BooleanFeature]
     */
    protected fun boolean(key: String): ReadOnlyProperty<Any?, BooleanFeature<C, M>> {
        return featureDelegate { BooleanFeature(key, module) }
    }

    /**
     * Creates a String feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [StringFeature]
     */
    protected fun string(key: String): ReadOnlyProperty<Any?, StringFeature<C, M>> {
        return featureDelegate { StringFeature(key, module) }
    }

    /**
     * Creates an Int feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns an [IntFeature]
     */
    protected fun int(key: String): ReadOnlyProperty<Any?, IntFeature<C, M>> {
        return featureDelegate { IntFeature(key, module) }
    }

    /**
     * Creates a Double feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [DoubleFeature]
     */
    protected fun double(key: String): ReadOnlyProperty<Any?, DoubleFeature<C, M>> {
        return featureDelegate { DoubleFeature(key, module) }
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
    ): ReadOnlyProperty<Any?, Feature.OfJsonObject<T, C, M>> {
        return featureDelegate { Feature.jsonObject(key, module) }
    }

    /**
     * Creates a custom wrapper type feature with automatic registration.
     *
     * @param T The wrapped type (e.g., LocalDateTime, UUID)
     * @param P The primitive type used for encoding (String, Int, etc.)
     * @param key The unique feature key
     * @return A delegated property that returns a [Feature.OfCustom]
     */
    protected inline fun <reified T : Any, reified P : Any> custom(
        key: String
    ): ReadOnlyProperty<Any?, Feature.OfCustom<T, P, C, M>> where
            P : EncodableValue<P> {
        return featureDelegate { Feature.custom(key, module) }
    }

    /**
     * Internal delegate factory that handles feature creation and registration.
     *
     * Features are created lazily on first property access and automatically
     * registered in the container's feature list.
     */
    private fun <F : Feature<*, *, C, M>> featureDelegate(
        factory: () -> F
    ): ReadOnlyProperty<Any?, F> {
        return object : ReadOnlyProperty<Any?, F> {
            private val feature by lazy {
                factory().also { _features.add(it) }
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): F = feature
        }
    }
}
