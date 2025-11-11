package io.amichne.konditional.core

import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.internal.builders.ConfigBuilder

/**
 * Configures feature flags using the DSL and loads them into the registry.
 *
 * This is the primary entry point for defining feature flag configurations.
 * The configuration is immediately loaded into the provided registry (or the default singleton).
 *
 * Example:
 * ```kotlin
 * config {
 *     MyFlags.FEATURE_A with {
 *         default(value = true)
 *         rule {
 *             platforms(Platform.IOS)
 *         } implies false
 *     }
 * }
 * ```
 *
 * @param registry The FlagRegistry to load the configuration into. Defaults to the singleton registry.
 * @param fn The DSL block for configuring flags. The receiver is [ConfigScope], a sealed interface
 *           that defines the public DSL API.
 */
@FeatureFlagDsl
inline fun config(registry: FlagRegistry = FlagRegistry, fn: ConfigScope.() -> Unit) {
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
