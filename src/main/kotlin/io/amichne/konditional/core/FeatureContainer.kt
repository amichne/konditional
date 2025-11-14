package io.amichne.konditional.core

import io.amichne.konditional.context.Context
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
abstract class FeatureContainer<M : Taxonomy>(
    protected val taxonomy: M,
) {
    private val _features = mutableListOf<Feature<*, *, *, M>>()

    /**
     * Returns all features declared in this container.
     * Features are registered lazily on first access.
     */
    fun allFeatures(): List<Feature<*, *, *, M>> = _features.toList()

    /**
     * Creates a Boolean feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [BooleanFeature]
     */
    protected fun <C : Context> boolean(
        flagScope: BooleanScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, BooleanFeature<C, M>> =
        ContainerFeaturePropertyDelegate { BooleanFeature(it, taxonomy) }

    /**
     * Creates a String feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [StringFeature]
     */
    protected fun <C : Context> string(
        stringScope: StringScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, StringFeature<C, M>> =
        ContainerFeaturePropertyDelegate { StringFeature(it, taxonomy) }

    /**
     * Creates an Int feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns an [IntFeature]
     */
    protected fun <C : Context> int(
        integerScope: IntegerScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, IntFeature<C, M>> =
        ContainerFeaturePropertyDelegate { IntFeature(it, taxonomy) }

    /**
     * Creates a Double feature with automatic registration.
     *
     * @param key The unique feature key
     * @return A delegated property that returns a [DoubleFeature]
     */
    protected fun <C : Context> double(
        decimalScope: DecimalScope<C, M>.() -> Unit,
    ): ReadOnlyProperty<FeatureContainer<M>, DoubleFeature<C, M>> =
        ContainerFeaturePropertyDelegate { DoubleFeature(it, taxonomy) }

    /**
     * Creates a JSON object feature with automatic registration.
     *
     * @param T The data class type
     * @param key The unique feature key
     * @return A delegated property that returns a [JsonEncodeableFeature]
     */
    protected inline fun <C : Context, reified T : Any> jsonObject(
        key: String,
    ): ReadOnlyProperty<FeatureContainer<M>, JsonFeature<C, M, T>> =
        ContainerFeaturePropertyDelegate { JsonFeature(key, taxonomy) }

    /**
     * Creates a custom wrapper type feature with automatic registration.
     *
     * @param T The wrapped type (e.g., LocalDateTime, UUID)
     * @param P The primitive type used for encoding (String, Int, etc.)
     * @param key The unique feature key
     * @return A delegated property that returns a [OfCustom]
     */
    protected inline fun <reified T : Any, reified P, C : Context> custom(
        key: String,
    ): ReadOnlyProperty<FeatureContainer<M>, OfCustom<T, P, C, M>> where
        P : Any, P : EncodableValue<P> = ContainerFeaturePropertyDelegate {
        Feature.custom(key, taxonomy)
    }

    /**
     * Internal delegate factory that handles feature creation and registration.
     *
     * Features are created lazily on first property access and automatically
     * registered in the container's feature list.
     */
    inner class ContainerFeaturePropertyDelegate<F : Feature<*, *, *, M>>(private val factory: M.(String) -> F) :
        ReadOnlyProperty<FeatureContainer<M>, F>, PropertyDelegateProvider<FeatureContainer<M>, F> {
        lateinit var name: String

        private val feature by lazy { factory(taxonomy, name).also { _features.add(it) } }

        override fun getValue(
            thisRef: FeatureContainer<M>,
            property: KProperty<*>,
        ): F = feature

        override fun provideDelegate(
            thisRef: FeatureContainer<M>,
            property: KProperty<*>,
        ): F = run {
            if (!::name.isInitialized) {
                name = property.name
            }
        }.let { feature }
    }
}
