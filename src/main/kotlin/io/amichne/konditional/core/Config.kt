package io.amichne.konditional.core

import io.amichne.konditional.core.dsl.ConfigScope
import io.amichne.konditional.core.dsl.FeatureFlagDsl
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.internal.builders.ConfigBuilder

/**
 * Configures feature flags for this taxonomy using the DSL.
 *
 * **DEPRECATED**: This function is deprecated in favor of using `FeatureContainer` with automatic
 * configuration through delegation. Configuration is now handled automatically when features are
 * accessed through `FeatureContainer`.
 *
 * **Migration**:
 * ```kotlin
 * // Old approach (deprecated)
 * Taxonomy.Domain.Payments.config {
 *     PaymentFeatures.APPLE_PAY with {
 *         default(false)
 *         rule { platforms(Platform.IOS) } implies true
 *     }
 * }
 *
 * // New approach (recommended)
 * object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
 *     val APPLE_PAY by boolean {
 *         default(false)
 *         rule { platforms(Platform.IOS) } implies true
 *     }
 * }
 * ```
 *
 * @param M The taxonomy type (inferred from receiver)
 * @param fn The DSL block for configuring flags. The receiver is [io.amichne.konditional.core.dsl.ConfigScope], a sealed interface
 *           that defines the public DSL API.
 */
@Deprecated(
    message = "Use FeatureContainer with delegation instead. Configuration is now handled automatically.",
    replaceWith = ReplaceWith(
        "object YourFeatures : FeatureContainer<M>(this) { val FEATURE by boolean { /* config */ } }",
        "io.amichne.konditional.core.features.FeatureContainer"
    ),
    level = DeprecationLevel.WARNING
)
@FeatureFlagDsl
inline fun <M : Taxonomy> M.config(registry: ModuleRegistry = this.registry, fn: ConfigScope.() -> Unit) {
    ConfigBuilder().apply(fn).build().let { registry.load(it) }
}

/**
 * Builds a Konfig configuration without loading it into any registry.
 *
 * This is useful for:
 * - Testing flag configurations
 * - Creating configurations for external snapshot management
 * - Serializing configurations without affecting the active registry
 *
 * Example:
 * ```kotlin
 * val konfig = buildSnapshot {
 *     MyFlags.FEATURE_A with {
 *         default(value = true)
 *     }
 * }
 * ```
 *
 * @param fn The DSL block for configuring flags. The receiver is [ConfigScope], a sealed interface
 *           that defines the public DSL API.
 * @return The built Konfig instance
 */
@FeatureFlagDsl
inline fun buildSnapshot(fn: ConfigScope.() -> Unit): Konfig =
    ConfigBuilder().apply(fn).build()
