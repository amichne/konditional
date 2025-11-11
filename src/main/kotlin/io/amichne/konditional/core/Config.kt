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
 * @param fn The DSL block for configuring flags
 */
@FeatureFlagDsl
fun config(registry: FlagRegistry = FlagRegistry, fn: ConfigBuilder.() -> Unit): Unit =
    ConfigBuilder.config(registry, fn)

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
 * @param fn The DSL block for configuring flags
 * @return The built Konfig instance
 */
@FeatureFlagDsl
fun buildSnapshot(fn: ConfigBuilder.() -> Unit): Konfig =
    ConfigBuilder.buildSnapshot(fn)
