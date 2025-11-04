package io.amichne.konditional.context

import com.squareup.moshi.JsonClass

// ---------- Semantic Version ----------
@JsonClass(generateAdapter = true)
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)

    companion object {
        fun of(
            major: Int,
            minor: Int,
            patch: Int,
        ): Version = Version(major, minor, patch)

        fun parse(raw: String): Version {
            val p = raw.split('.')
            require(p.isNotEmpty() && p.size <= 3) { "Bad versions: $raw" }
            val m = p.getOrNull(0)?.toIntOrNull() ?: 0
            val n = p.getOrNull(1)?.toIntOrNull() ?: 0
            val c = p.getOrNull(2)?.toIntOrNull() ?: 0
            return Version(m, n, c)
        }

        val default = Version(-1, -1, -1)
    }
}
