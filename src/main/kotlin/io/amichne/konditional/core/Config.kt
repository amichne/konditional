package io.amichne.konditional.core

import io.amichne.konditional.core.dsl.ConfigScope
import io.amichne.konditional.core.dsl.FeatureFlagDsl
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.internal.builders.ConfigBuilder

/**
 * Configures feature flags for this taxonomy using the DSL.
 *
 * This is the primary entry point for defining taxonomy-specific feature flag configurations.
 * The configuration is immediately loaded into the taxonomy's isolated registry.
 *
 * ## Taxonomy-Scoped Configuration
 *
 * Each taxonomy configures only its own flags, ensuring complete isolation:
 *
 * ```kotlin
 * // Configure Domain A's flags
 * Taxonomy.Domain.TeamA.config {
 *     TeamAFeatures.FEATURE_X with {
 *         default(value = true)
 *         rule {
 *             platforms(Platform.IOS)
 *         } implies false
 *     }
 * }
 *
 * // Configure Domain B's flags (completely isolated)
 * Taxonomy.Domain.TeamB.config {
 *     TeamBFeatures.FEATURE_Y with {
 *         default(value = false)
 *     }
 * }
 * ```
 *
 * ## Benefits
 *
 * - **Zero shared files**: Teams never touch the same configuration code
 * - **Compile-time isolation**: Cannot accidentally configure another taxonomy's flags
 * - **Runtime isolation**: Each taxonomy has its own registry instance
 * - **Independent deployment**: Taxonomy configs can be deployed separately
 *
 * @param M The taxonomy type (inferred from receiver)
 * @param fn The DSL block for configuring flags. The receiver is [io.amichne.konditional.core.dsl.ConfigScope], a sealed interface
 *           that defines the public DSL API.
 */
@FeatureFlagDsl
inline fun <M : Taxonomy> M.config(registry: ModuleRegistry = this.registry, fn: ConfigScope.() -> Unit) {
    ConfigBuilder().apply(fn).build().let { registry.load(it) }
}

@Deprecated(message = "Replace with receiver varient", replaceWith = ReplaceWith("Taxonomy.Core.config(fn)"))
fun config(fn: ConfigScope.() -> Unit) {
    ConfigBuilder().apply(fn).build().let { Taxonomy.Core.registry.load(it) }
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
