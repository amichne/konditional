@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.internal.serialization.models.SerializableSnapshotMetadata
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.uispec.JsonPointer
import io.amichne.konditional.uispec.UiArray
import io.amichne.konditional.uispec.UiBoolean
import io.amichne.konditional.uispec.UiDouble
import io.amichne.konditional.uispec.UiEnum
import io.amichne.konditional.uispec.UiInt
import io.amichne.konditional.uispec.UiMap
import io.amichne.konditional.uispec.UiNull
import io.amichne.konditional.uispec.UiObject
import io.amichne.konditional.uispec.UiString
import io.amichne.konditional.uispec.UiValue

class KonditionalSnapshotValueProvider(
    private val snapshot: SerializableSnapshot,
) : UiValueProvider {
    override fun read(pointer: JsonPointer): UiValue? =
        pointer.tokens().let { tokens ->
            when (tokens.firstOrNull()) {
                "flags" -> readFlags(tokens.drop(1))
                "meta" -> readMeta(snapshot.meta, tokens.drop(1))
                else -> null
            }
        }

    private fun readFlags(tokens: List<String>): UiValue? =
        if (tokens.isEmpty()) {
            UiArray(snapshot.flags.map { UiNull })
        } else {
            tokens.firstOrNull()
                ?.toIndexOrNull()
                ?.let { index -> snapshot.flags.getOrNull(index) }
                ?.let { flag -> readFlag(flag, tokens.drop(1)) }
        }

    private fun readFlag(flag: SerializableFlag, tokens: List<String>): UiValue? =
        tokens.firstOrNull()?.let { segment ->
            when (segment) {
                "key" -> UiString(flag.key.toString())
                "defaultValue" -> readFlagValue(flag.defaultValue, tokens.drop(1))
                "salt" -> UiString(flag.salt)
                "isActive" -> UiBoolean(flag.isActive)
                "rampUpAllowlist" -> readStringSet(flag.rampUpAllowlist, tokens.drop(1))
                "rules" -> readRules(flag.rules, tokens.drop(1))
                else -> null
            }
        }

    private fun readRules(rules: List<SerializableRule>, tokens: List<String>): UiValue? =
        if (tokens.isEmpty()) {
            UiArray(rules.map { UiNull })
        } else {
            tokens.firstOrNull()
                ?.toIndexOrNull()
                ?.let { index -> rules.getOrNull(index) }
                ?.let { rule -> readRule(rule, tokens.drop(1)) }
        }

    private fun readRule(rule: SerializableRule, tokens: List<String>): UiValue? =
        tokens.firstOrNull()?.let { segment ->
            when (segment) {
                "value" -> readFlagValue(rule.value, tokens.drop(1))
                "rampUp" -> rampUpValue(rule.rampUp)
                "rampUpAllowlist" -> readStringSet(rule.rampUpAllowlist, tokens.drop(1))
                "note" -> rule.note?.let(::UiString) ?: UiNull
                "locales" -> readStringSet(rule.locales, tokens.drop(1))
                "platforms" -> readStringSet(rule.platforms, tokens.drop(1))
                "versionRange" -> readVersionRange(rule.versionRange, tokens.drop(1))
                "axes" -> readAxes(rule.axes, tokens.drop(1))
                else -> null
            }
        }

    private fun readFlagValue(value: FlagValue<*>, tokens: List<String>): UiValue? =
        if (tokens.isEmpty()) {
            flagValuePayload(value)
        } else {
            tokens.firstOrNull()?.let { segment ->
                when (segment) {
                    "type" -> UiEnum(value.toValueType().name)
                    "value" -> flagValuePayload(value)
                    "enumClassName" -> (value as? FlagValue.EnumValue)?.enumClassName?.let(::UiString) ?: UiNull
                    "dataClassName" -> (value as? FlagValue.DataClassValue)?.dataClassName?.let(::UiString) ?: UiNull
                    else -> null
                }
            }
        }

    private fun flagValuePayload(value: FlagValue<*>): UiValue =
        when (value) {
            is FlagValue.BooleanValue -> UiBoolean(value.value)
            is FlagValue.StringValue -> UiString(value.value)
            is FlagValue.IntValue -> UiInt(value.value.toLong())
            is FlagValue.DoubleValue -> UiDouble(value.value)
            is FlagValue.EnumValue -> UiEnum(value.value)
            is FlagValue.DataClassValue -> UiObject(value.value.mapValues { (_, v) -> toUiValue(v) })
        }

    private fun readVersionRange(range: VersionRange?, tokens: List<String>): UiValue? =
        range?.let { versionRange ->
            if (tokens.isEmpty()) {
                UiObject(
                    mapOf(
                        "type" to UiEnum(versionRange.type.name),
                        "min" to (versionRange.min?.let(::versionToUiValue) ?: UiNull),
                        "max" to (versionRange.max?.let(::versionToUiValue) ?: UiNull),
                    ),
                )
            } else {
                when (tokens.firstOrNull()) {
                    "type" -> UiEnum(versionRange.type.name)
                    "min" -> readVersion(versionRange.min, tokens.drop(1))
                    "max" -> readVersion(versionRange.max, tokens.drop(1))
                    else -> null
                }
            }
        }

    private fun readVersion(version: io.amichne.konditional.context.Version?, tokens: List<String>): UiValue? =
        if (version == null) {
            UiNull
        } else {
            when (tokens.firstOrNull()) {
                "major" -> UiInt(version.major.toLong())
                "minor" -> UiInt(version.minor.toLong())
                "patch" -> UiInt(version.patch.toLong())
                null -> versionToUiValue(version)
                else -> null
            }
        }

    private fun versionToUiValue(version: io.amichne.konditional.context.Version): UiValue =
        UiObject(
            mapOf(
                "major" to UiInt(version.major.toLong()),
                "minor" to UiInt(version.minor.toLong()),
                "patch" to UiInt(version.patch.toLong()),
            ),
        )

    private fun readAxes(axes: Map<String, Set<String>>, tokens: List<String>): UiValue? =
        if (tokens.isEmpty()) {
            UiMap(axes.mapValues { (_, values) -> UiArray(values.sorted().map(::UiString)) })
        } else {
            tokens.firstOrNull()?.let { key ->
                axes[key]?.let { values -> UiArray(values.sorted().map(::UiString)) }
            }
        }

    private fun readStringSet(values: Set<String>, tokens: List<String>): UiValue? =
        values.sorted().let { ordered ->
            if (tokens.isEmpty()) {
                UiArray(ordered.map(::UiString))
            } else {
                tokens.firstOrNull()
                    ?.toIndexOrNull()
                    ?.let { index -> ordered.getOrNull(index) }
                    ?.let(::UiString)
            }
        }

    private fun readMeta(meta: SerializableSnapshotMetadata?, tokens: List<String>): UiValue? =
        meta?.let { snapshotMeta ->
            if (tokens.isEmpty()) {
                UiObject(
                    mapOf(
                        "version" to (snapshotMeta.version?.let(::UiString) ?: UiNull),
                        "generatedAtEpochMillis" to (
                            snapshotMeta.generatedAtEpochMillis?.let { UiInt(it) } ?: UiNull
                        ),
                        "source" to (snapshotMeta.source?.let(::UiString) ?: UiNull),
                    ),
                )
            } else {
                when (tokens.firstOrNull()) {
                    "version" -> snapshotMeta.version?.let(::UiString) ?: UiNull
                    "generatedAtEpochMillis" -> snapshotMeta.generatedAtEpochMillis?.let { UiInt(it) } ?: UiNull
                    "source" -> snapshotMeta.source?.let(::UiString) ?: UiNull
                    else -> null
                }
            }
        }

    private fun toUiValue(value: Any?): UiValue =
        when (value) {
            null -> UiNull
            is UiValue -> value
            is Boolean -> UiBoolean(value)
            is String -> UiString(value)
            is Int -> UiInt(value.toLong())
            is Long -> UiInt(value)
            is Double -> UiDouble(value)
            is Float -> UiDouble(value.toDouble())
            is Enum<*> -> UiEnum(value.name)
            is Map<*, *> -> UiObject(
                value.entries.associate { entry ->
                    entry.key.toString() to toUiValue(entry.value)
                },
            )
            is Iterable<*> -> UiArray(value.map { element -> toUiValue(element) })
            else -> UiString(value.toString())
        }

    private fun rampUpValue(value: Double): UiValue =
        if (value % 1.0 == 0.0) {
            UiInt(value.toLong())
        } else {
            UiDouble(value)
        }

    private fun String.toIndexOrNull(): Int? = toIntOrNull()

    private fun JsonPointer.tokens(): List<String> =
        value.split("/").filter { it.isNotBlank() }.map { segment ->
            segment.replace("~1", "/").replace("~0", "~")
        }
}
