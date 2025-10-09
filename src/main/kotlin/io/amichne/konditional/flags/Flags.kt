@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.amichne.konditional.flags

import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

// ---------- Locale and Market enums (examples only) ----------
enum class AppLocale(
    val tag: String,
    val language: String,
    val region: String
) {
    EN_US("en-US", "en", "US"),
    ES_US("es-US", "es", "US"),
    EN_CA("en-CA", "en", "CA"),
    HI_IN("hi-IN", "hi", "IN"),
}

enum class Market(
    val region: String,
    val locales: Set<AppLocale>
) {
    US("US", setOf(AppLocale.EN_US, AppLocale.ES_US)),
    CA("CA", setOf(AppLocale.EN_CA)),
    IN("IN", setOf(AppLocale.HI_IN));
}

// ---------- Platforms ----------
enum class Platform { IOS, ANDROID }

// ---------- Semantic Version ----------
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)

    companion object {
        fun parse(raw: String): Version {
            val p = raw.split('.')
            require(p.isNotEmpty() && p.size <= 3) { "Bad version: $raw" }
            val m = p.getOrNull(0)?.toIntOrNull() ?: 0
            val n = p.getOrNull(1)?.toIntOrNull() ?: 0
            val c = p.getOrNull(2)?.toIntOrNull() ?: 0
            return Version(m, n, c)
        }
    }
}

data class VersionRange(
    val min: Version? = null,
    val max: Version? = null
) {
    fun contains(v: Version): Boolean =
        (min?.let { v >= it } ?: true) && (max?.let { v <= it } ?: true)

    fun hasBounds(): Boolean = (min != null) || (max != null)
}

@JvmInline
@OptIn(ExperimentalStdlibApi::class)
value class HexId internal constructor(internal val externalId: String) {
    internal val byteId: ByteArray
        get() = externalId.hexToByteArray(HexFormat.Default)

    val id: String
        get() = byteId.toHexString(HexFormat.Default)

    init {
        require(id == externalId)
    }
}

// ---------- Stable identity ----------
sealed interface StableId {
    val hexId: HexId
    val id: String

    companion object {
        fun of(id: String): StableId = Factory.Instance(HexId(id))
    }

    private object Factory {
        //        @ConsistentCopyVisibility
        data class Instance(override val hexId: HexId) : StableId {
            override val id: String
                get() = hexId.id
        }
    }
}

// ---------- Evaluation context ----------
data class EvalContext(
    val locale: AppLocale,
    val platform: Platform,
    val appVersion: Version,
    val stableId: StableId
) {
    val market: Market = when (locale.region) {
        "US" -> Market.US
        "CA" -> Market.CA
        "IN" -> Market.IN
        else -> error("Unsupported region: ${locale.region}")
    }
}

// ---------- Rule / Flag model ----------
data class Rule(
    val value: Boolean,
    val coveragePct: Double = if (value) 100.0 else 0.0, // [0,100]
    val markets: Set<Market> = emptySet(),
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = VersionRange(),
    val note: String? = null
) {
    init {
        require(coveragePct in 0.0..100.0) { "coveragePct out of range" }
    }

    fun matches(ctx: EvalContext): Boolean =
        (markets.isEmpty() || ctx.market in markets) &&
        (locales.isEmpty() || ctx.locale in locales) &&
        (platforms.isEmpty() || ctx.platform in platforms) &&
        (!versionRange.hasBounds() || versionRange.contains(ctx.appVersion))

    fun specificity(): Int =
        (if (markets.isNotEmpty()) 1 else 0) +
        (if (locales.isNotEmpty()) 1 else 0) +
        (if (platforms.isNotEmpty()) 1 else 0) +
        (if (versionRange.hasBounds()) 1 else 0)
}

data class Flag(
    val key: String,
    val rules: List<Rule>,
    val defaultValue: Boolean = false,
    /**
     * Percentage of users that should receive `true` when no rules match.
     * Defaults to 100 when `defaultValue` is true, otherwise 0.
     */
    val defaultCoveragePct: Double = if (defaultValue) 100.0 else 0.0,
    val salt: String = "v1"
) {
    init {
        require(defaultCoveragePct in 0.0..100.0)
    }

    private val orderedRules: List<Rule> =
        rules.sortedWith(compareByDescending<Rule> { it.specificity() }.thenBy { it.note ?: "" })

    fun evaluate(context: EvalContext): Boolean {
        for (r in orderedRules) {
            if (!r.matches(context)) continue
            if (inBucket(key, context.stableId.hexId, salt, r.coveragePct)) return r.value
        }
        return when {
            defaultCoveragePct <= 0.0 -> false
            defaultCoveragePct >= 100.0 -> true
            else -> inBucket("$key#default", context.stableId.hexId, salt, defaultCoveragePct)
        }
    }
}

// ---------- Deterministic bucketing ----------
private fun inBucket(
    flagKey: String,
    id: HexId,
    salt: String,
    coveragePct: Double
): Boolean {
    if (coveragePct <= 0.0) return false
    if (coveragePct >= 100.0) return true
    val bucket = stableBucket(flagKey, id, salt) // 0..9999
    val threshold = (coveragePct * 100).roundToInt() // 2-decimal precision
    return bucket < threshold
}

private fun stableBucket(
    flagKey: String,
    id: HexId,
    salt: String
): Int {
    val bytes = sha256("$salt:$flagKey:${id.id}")
    val v = ((bytes[0].toInt() and 0xFF) shl 24) or
        ((bytes[1].toInt() and 0xFF) shl 16) or
        ((bytes[2].toInt() and 0xFF) shl 8) or
        (bytes[3].toInt() and 0xFF)
    val u = v.toLong() and 0xFFFF_FFFFL
    return (u % 10_000L).toInt()
}

private fun sha256(s: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))

// ---------- Registry and DSL ----------
object Flags {
    private val snapshot = AtomicReference(Registry(emptyMap()))

    data class Registry(val flags: Map<String, Flag>)

    fun load(config: Registry) {
        snapshot.set(config)
    }

    fun eval(
        key: String,
        ctx: EvalContext
    ): Boolean =
        snapshot.get().flags[key]?.evaluate(ctx) ?: false

    fun evalAll(ctx: EvalContext): Map<String, Boolean> =
        snapshot.get().flags.mapValues { (_, f) -> f.evaluate(ctx) }
}

class ConfigBuilder {
    private val flags = LinkedHashMap<String, Flag>()
    fun flag(
        key: String,
        build: FlagBuilder.() -> Unit
    ) {
        require(key.isNotBlank())
        require(key !in flags) { "Duplicate flag $key" }
        flags[key] = FlagBuilder(key).apply(build).build()
    }

    fun build(): Flags.Registry = Flags.Registry(flags.toMap())
}

class FlagBuilder(private val key: String) {
    private val rules = mutableListOf<Rule>()
    private var defaultValue: Boolean = false
    private var defaultCoverage: Double? = null
    private var salt: String = "v1"

    fun default(
        value: Boolean,
        coverage: Double? = null
    ) {
        defaultValue = value
        defaultCoverage = coverage
    }

    fun salt(value: String) {
        salt = value
    }

    fun rule(build: RuleBuilder.() -> Unit) {
        rules += RuleBuilder().apply(build).build()
    }

    fun build(): Flag = Flag(
        key = key,
        rules = rules.toList(),
        defaultValue = defaultValue,
        defaultCoveragePct = defaultCoverage ?: if (defaultValue) 100.0 else 0.0,
        salt = salt
    )
}

class RuleBuilder {
    private var value: Boolean = false
    private var coverage: Double? = null
    private val markets = linkedSetOf<Market>()
    private val locales = linkedSetOf<AppLocale>()
    private val platforms = linkedSetOf<Platform>()
    private var range: VersionRange = VersionRange()
    private var note: String? = null

    fun value(
        v: Boolean,
        coveragePct: Double? = null
    ) {
        value = v; coverage = coveragePct
    }

    fun markets(vararg ms: Market) {
        markets += ms
    }

    fun locales(vararg ls: AppLocale) {
        locales += ls
    }

    fun platforms(vararg ps: Platform) {
        platforms += ps
    }

    fun versions(
        min: String? = null,
        max: String? = null
    ) {
        range = VersionRange(min?.let(Version::parse), max?.let(Version::parse))
    }

    fun note(text: String) {
        note = text
    }

    fun build(): Rule = Rule(
        value = value,
        coveragePct = coverage ?: if (value) 100.0 else 0.0,
        markets = markets,
        locales = locales,
        platforms = platforms,
        versionRange = range,
        note = note
    )
}

// ---------- Example bootstrap ----------
fun bootstrapExample() {
    val registry = ConfigBuilder().apply {
        flag("enable_compact_cards") {
            default(value = false)
            rule {
                markets(Market.US); platforms(Platform.IOS); versions(min = "7.10.0")
                value(true, coveragePct = 50.0); note("US iOS staged rollout")
            }
            rule {
                locales(AppLocale.HI_IN); value(true); note("IN Hindi full")
            }
        }
        flag("use_lightweight_home") {
            default(value = true, coverage = 100.0)
            rule {
                platforms(Platform.ANDROID); versions(max = "6.4.99")
                value(false, coveragePct = 100.0); note("Android legacy off")
            }
        }
    }.build()
    Flags.load(registry)
}
