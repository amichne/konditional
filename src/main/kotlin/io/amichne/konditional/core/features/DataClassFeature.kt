package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.konditional.core.types.DataClassEncodeable
import io.amichne.kontracts.schema.ObjectSchema

/**
 * Sealed interface for custom encodeable feature flags.
 *
 * DataClassFeature (despite its name) allows using any custom encodeable type as feature flag values,
 * providing structured, type-safe configuration with full schema validation.
 *
 * **Note:** This interface name is retained for backwards compatibility but works with
 * [KotlinEncodeable] types. Consider using the term "custom feature" in new documentation.
 *
 * Example:
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
 * object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
 *     val PAYMENT_CONFIG by custom(
 *         default = PaymentConfig()
 *     ) {
 *         rule {
 *             environments(Environment.PRODUCTION)
 *         } returns PaymentConfig(maxRetries = 5, timeout = 60.0)
 *     }
 * }
 * ```
 *
 * @param T The custom type implementing [KotlinEncodeable]<[ObjectSchema]>
 * @param C The context type used for evaluation
 * @param M The namespace this feature belongs to
 */
sealed interface DataClassFeature<T : KotlinEncodeable<ObjectSchema>, C : Context, M : Namespace> :
    Feature<DataClassEncodeable<T>, T, C, M> {

    companion object {
        /**
         * Factory function for creating DataClassFeature instances.
         *
         * @param key The feature key (usually the property name)
         * @param module The namespace this feature belongs to
         * @return A DataClassFeature instance
         */
        operator fun <T : KotlinEncodeable<ObjectSchema>, C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): DataClassFeature<T, C, M> =
            DataClassFeatureImpl(key, module)

        @PublishedApi
        internal data class DataClassFeatureImpl<T : KotlinEncodeable<ObjectSchema>, C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : DataClassFeature<T, C, M>
    }
}
