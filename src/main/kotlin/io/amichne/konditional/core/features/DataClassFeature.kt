package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.DataClassEncodeable
import io.amichne.konditional.core.types.DataClassWithSchema

/**
 * Sealed interface for data class-based feature flags.
 *
 * DataClassFeature allows using custom data classes as feature flag values,
 * providing structured, type-safe configuration with full schema validation.
 *
 * Example:
 * ```kotlin
 * @ConfigDataClass
 * data class PaymentConfig(
 *     val maxRetries: Int = 3,
 *     val timeout: Double = 30.0,
 *     val enabled: Boolean = true
 * ) : DataClassWithSchema
 *
 * object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
 *     val PAYMENT_CONFIG by dataClass(
 *         default = PaymentConfig()
 *     ) {
 *         rule {
 *             environments(Environment.PRODUCTION)
 *         } returns PaymentConfig(maxRetries = 5, timeout = 60.0)
 *     }
 * }
 * ```
 *
 * @param T The data class type implementing DataClassWithSchema
 * @param C The context type used for evaluation
 * @param M The namespace this feature belongs to
 */
sealed interface DataClassFeature<T : DataClassWithSchema, C : Context, M : Namespace> :
    Feature<DataClassEncodeable<T>, T, C, M> {

    companion object {
        /**
         * Factory function for creating DataClassFeature instances.
         *
         * @param key The feature key (usually the property name)
         * @param module The namespace this feature belongs to
         * @return A DataClassFeature instance
         */
        operator fun <T : DataClassWithSchema, C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): DataClassFeature<T, C, M> =
            DataClassFeatureImpl(key, module)

        @PublishedApi
        internal data class DataClassFeatureImpl<T : DataClassWithSchema, C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : DataClassFeature<T, C, M>
    }
}
