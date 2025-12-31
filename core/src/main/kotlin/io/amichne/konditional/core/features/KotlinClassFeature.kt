package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.KotlinEncodeable

/**
 * Sealed interface for custom encodeable feature flags.
 *
 * KotlinClassFeature (despite its name) allows using any custom encodeable type as feature flag values,
 * providing structured, type-safe configuration with full schema validation.
 *
 * Example:
 * ```kotlin
 * data class PaymentConfig(
 *     val maxRetries: Int = 3,
 *     val timeout: Double = 30.0,
 *     val enabled: Boolean = true
 * ) : KotlinEncodeable<ObjectSchema> {
 *     override val schema = schemaRoot {
 *         ::maxRetries of { minimum = 0 }
 *         ::timeout of { minimum = 0.0 }
 *         ::enabled of { default = true }
 *     }
 * }
 *
 * object Payments : Namespace("payments") {
 *     val PAYMENT_CONFIG by custom(default = PaymentConfig()) {
 *         rule(PaymentConfig(maxRetries = 5, timeout = 60.0)) { platforms(Platform.WEB) }
 *     }
 * }
 * ```
 *
 * @param T The custom type implementing [KotlinEncodeable]
 * @param C The context type used for evaluation
 * @param M The namespace this feature belongs to
 */
sealed interface KotlinClassFeature<T : KotlinEncodeable<*>, C : Context, M : Namespace> :
    Feature<T, C, M> {

    companion object {
        /**
         * Factory function for creating KotlinClassFeature instances.
         *
         * @param key The feature key (usually the property name)
         * @param module The namespace this feature belongs to
         * @return A KotlinClassFeature instance
         */
        operator fun <T : KotlinEncodeable<*>, C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): KotlinClassFeature<T, C, M> =
            KotlinClassFeatureImpl(key, module)

        @PublishedApi
        internal data class KotlinClassFeatureImpl<T : KotlinEncodeable<*>, C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : KotlinClassFeature<T, C, M>, Identifiable by Identifiable(key, namespace)
    }
}
