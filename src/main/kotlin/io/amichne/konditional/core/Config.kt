package io.amichne.konditional.core

import io.amichne.konditional.core.dsl.ConfigScope
import io.amichne.konditional.core.dsl.FeatureFlagDsl
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.internal.builders.ConfigBuilder

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
