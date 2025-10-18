package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Platform
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

// ---------- Rule / Condition model ----------

/**
 * Rule
 *
 * @param T
 * @param S
 * @property value
 * @property rampUp
 * @property locales
 * @property platforms
 * @property versionRange
 * @property note
 * @constructor Create empty Rule
 *
 * TODO - Update to S, can bubble up through builder, ultimately we resolve this to <S> in Condition
 *
 * @see io.amichne.konditional.core.Flags
 */
data class Rule(
    val rampUp: RampUp,
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded,
    val note: String? = null,
) {
    init {
        require(rampUp.value in 0.0..100.0) { "coveragePct out of range" }
    }

    fun matches(context: Context): Boolean =
        (locales.isEmpty() || context.locale in locales) &&
            (platforms.isEmpty() || context.platform in platforms) &&
            (!versionRange.hasBounds() || versionRange.contains(context.appVersion))

    fun specificity(): Int =
        (if (locales.isNotEmpty()) 1 else 0) +
            (if (platforms.isNotEmpty()) 1 else 0) +
            (if (versionRange.hasBounds()) 1 else 0)
}
