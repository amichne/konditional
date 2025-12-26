package io.amichne.konditional.demo.configpages

import io.amichne.konditional.configstate.ConfigurationStateFactory
import io.amichne.konditional.configstate.EnumOptionsDescriptor
import io.amichne.konditional.configstate.FieldType
import io.amichne.konditional.configstate.MapConstraintsDescriptor
import io.amichne.konditional.configstate.NumberRangeDescriptor
import io.amichne.konditional.configstate.SemverConstraintsDescriptor
import io.amichne.konditional.configstate.StringConstraintsDescriptor
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.demo.DemoFeatures
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.serialization.SnapshotSerializer
import io.amichne.konditional.values.FeatureId
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.option
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.stream.createHTML
import kotlinx.html.textArea
import kotlinx.html.title
import kotlinx.html.unsafe
import io.amichne.konditional.context.Version
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.VersionRange
import kotlin.text.Charsets.UTF_8

fun Routing.configPagesRoutes() {
    get("/config-pages") {
        call.respondHtml {
            renderConfigPagesShell()
        }
    }

    get("/config-pages/fragments/flag-list") {
        val filter = call.request.queryParameters["filter"].orEmpty()
        call.respondText(
            text = ConfigPagesFragments.flagList(filter = filter),
            contentType = ContentType.Text.Html,
        )
    }

    get("/config-pages/fragments/flag-detail") {
        val key = call.request.queryParameters["key"].orEmpty()
        val filter = call.request.queryParameters["filter"].orEmpty()
        call.respondFlagDetailFragment(key = key, filter = filter)
    }

    post("/config-pages/actions/flag/active") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val isActive = params["isActive"]?.toBooleanStrictOrNull()

        if (isActive == null) {
            call.respondFlagDetailFragment(
                key = key,
                filter = filter,
                status = HttpStatusCode.UnprocessableEntity,
                error = "Invalid 'isActive' value",
            )
        } else {
            val result = ConfigPagesMutations.updateActive(key = key, isActive = isActive)
            call.respondMutationResult(
                key = key,
                filter = filter,
                result = result,
                successToast = "Updated active state",
            )
        }
    }

    post("/config-pages/actions/flag/default") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val result = ConfigPagesMutations.updateDefaultValueJson(key = key, params = params)

        call.respondMutationResult(
            key = key,
            filter = filter,
            result = result,
            successToast = "Updated value",
        )
    }

    post("/config-pages/actions/flag/salt") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val result = ConfigPagesMutations.updateSalt(key = key, params = params)

        call.respondMutationResult(
            key = key,
            filter = filter,
            result = result,
            successToast = "Updated salt",
        )
    }

    post("/config-pages/actions/flag/allowlist") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val result = ConfigPagesMutations.updateRampUpAllowlist(key = key, params = params)

        call.respondMutationResult(
            key = key,
            filter = filter,
            result = result,
            successToast = "Updated allowlist",
        )
    }

    post("/config-pages/actions/rule/add") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val result = ConfigPagesMutations.addRule(key = key)

        call.respondMutationResult(
            key = key,
            filter = filter,
            result = result,
            successToast = "Added rule",
        )
    }

    post("/config-pages/actions/rule/remove") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val result = ConfigPagesMutations.removeRule(key = key, params = params)

        call.respondMutationResult(
            key = key,
            filter = filter,
            result = result,
            successToast = "Removed rule",
        )
    }

    post("/config-pages/actions/rule/update") {
        val params = call.receiveParameters()
        val key = params["key"].orEmpty()
        val filter = params["filter"].orEmpty()
        val result = ConfigPagesMutations.updateRule(key = key, params = params)

        call.respondMutationResult(
            key = key,
            filter = filter,
            result = result,
            successToast = "Updated rule",
        )
    }

    get("/config-pages/fragments/toast/clear") {
        call.respondText(
            text = ConfigPagesFragments.toast(message = null, oob = false),
            contentType = ContentType.Text.Html,
        )
    }
}

private suspend fun ApplicationCall.respondMutationResult(
    key: String,
    filter: String,
    result: MutationOutcome,
    successToast: String,
) {
    when (result) {
        is MutationOutcome.Success -> {
            DemoFeatures.load(result.configuration)
            response.header("HX-Trigger", """{"configPagesUpdated":{}}""")
            respondFlagDetailFragment(
                key = key,
                filter = filter,
                extraHtml = ConfigPagesFragments.toast(message = successToast, oob = true),
            )
        }

        is MutationOutcome.Failure -> {
            respondFlagDetailFragment(
                key = key,
                filter = filter,
                status = HttpStatusCode.UnprocessableEntity,
                error = result.message,
                extraHtml = ConfigPagesFragments.toast(message = "Update rejected", oob = true),
            )
        }
    }
}

private suspend fun ApplicationCall.respondFlagDetailFragment(
    key: String,
    filter: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    error: String? = null,
    extraHtml: String? = null,
) {
    val normalized = runCatching { FeatureId.parse(key) }.getOrNull()
    val response = ConfigurationStateFactory.from(DemoFeatures.configuration)
    val flag = normalized?.let { id -> response.currentState.flags.firstOrNull { it.key == id } }
    val supported = response.supportedValues

    if (flag == null) {
        respondText(
            contentType = ContentType.Text.Html,
            status = HttpStatusCode.NotFound,
            text =
                (extraHtml.orEmpty() + ConfigPagesFragments.flagDetailNotFound(key = key))
                    .ifBlank { ConfigPagesFragments.flagDetailNotFound(key = key) },
        )
    } else {
        respondText(
            contentType = ContentType.Text.Html,
            status = status,
            text =
                (extraHtml.orEmpty() + ConfigPagesFragments.flagDetail(flag = flag, supported = supported, filter = filter, error = error))
                    .ifBlank { ConfigPagesFragments.flagDetail(flag = flag, supported = supported, filter = filter, error = error) },
        )
    }
}

private object ConfigPagesMutations {
    private val patchAdapter =
        SnapshotSerializer
            .defaultMoshi()
            .adapter(SerializablePatch::class.java)
            .indent("  ")

    fun updateActive(key: String, isActive: Boolean): MutationOutcome =
        updateFlag(key) { current ->
            FlagUpdate.Updated(current.copy(isActive = isActive))
        }

    fun updateDefaultValueJson(key: String, params: io.ktor.http.Parameters): MutationOutcome =
        updateFlag(key) { current ->
            val json = params["valueJson"].orEmpty().trim()
            when (val parsed = parseFlagValueJson(json)) {
                is FlagValueParseOutcome.Success -> FlagUpdate.Updated(current.copy(defaultValue = parsed.value))
                is FlagValueParseOutcome.Failure -> FlagUpdate.Rejected(parsed.message)
            }
        }

    fun updateSalt(key: String, params: io.ktor.http.Parameters): MutationOutcome =
        updateFlag(key) { current ->
            val response = ConfigurationStateFactory.from(DemoFeatures.configuration)
            val descriptor = response.supportedValues.byType[FieldType.SALT.name] as? StringConstraintsDescriptor
            val raw = params["salt"].orEmpty()
            val trimmed = raw.trim()

            val error = validateString(trimmed, descriptor)
            error?.let(FlagUpdate::Rejected) ?: FlagUpdate.Updated(current.copy(salt = trimmed))
        }

    fun updateRampUpAllowlist(key: String, params: io.ktor.http.Parameters): MutationOutcome =
        updateFlag(key) { current ->
            val response = ConfigurationStateFactory.from(DemoFeatures.configuration)
            val descriptor = response.supportedValues.byType[FieldType.RAMP_UP_ALLOWLIST.name] as? StringConstraintsDescriptor
            val raw = params["allowlist"].orEmpty()
            val tokens =
                raw.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .toSet()

            val error = validateStringSet(tokens, descriptor)
            error?.let(FlagUpdate::Rejected) ?: FlagUpdate.Updated(current.copy(rampUpAllowlist = tokens))
        }

    fun addRule(key: String): MutationOutcome =
        updateFlag(key) { current ->
            val rule =
                SerializableRule(
                    value = current.defaultValue,
                    rampUp = 100.0,
                    rampUpAllowlist = emptySet(),
                    note = null,
                    locales = emptySet(),
                    platforms = emptySet(),
                    versionRange = null,
                    axes = emptyMap(),
                )

            FlagUpdate.Updated(current.copy(rules = current.rules + rule))
        }

    fun removeRule(key: String, params: io.ktor.http.Parameters): MutationOutcome =
        updateFlag(key) { current ->
            val index = params["index"]?.toIntOrNull()
            val updated =
                if (index == null || index !in current.rules.indices) {
                    null
                } else {
                    current.rules.toMutableList().also { it.removeAt(index) }.toList()
                }

            updated?.let { FlagUpdate.Updated(current.copy(rules = it)) }
                ?: FlagUpdate.Rejected("Invalid rule index")
        }

    fun updateRule(key: String, params: io.ktor.http.Parameters): MutationOutcome =
        updateFlag(key) { current ->
            val supported = ConfigurationStateFactory.from(DemoFeatures.configuration).supportedValues
            val index = params["index"]?.toIntOrNull()
            val existing = index?.let { current.rules.getOrNull(it) }

            if (index == null || existing == null) {
                return@updateFlag FlagUpdate.Rejected("Invalid rule index")
            }

            val updatedOrError = updateRuleFromParams(existing = existing, params = params, supported = supported)
            when (updatedOrError) {
                is RuleUpdateOutcome.Rejected -> FlagUpdate.Rejected(updatedOrError.message)
                is RuleUpdateOutcome.Updated -> {
                    val rules = current.rules.toMutableList().also { it[index] = updatedOrError.rule }.toList()
                    FlagUpdate.Updated(current.copy(rules = rules))
                }
            }
        }

    private fun parseFlagValueJson(json: String): FlagValueParseOutcome {
        val trimmed = json.trim()
        return try {
            if (trimmed.isBlank()) {
                FlagValueParseOutcome.Failure("valueJson is empty")
            } else {
                val moshi = SnapshotSerializer.defaultMoshi()
                val adapter = moshi.adapter(FlagValue::class.java)
                val parsed = adapter.fromJson(trimmed)
                parsed?.let(FlagValueParseOutcome::Success)
                    ?: FlagValueParseOutcome.Failure("valueJson parsed to null")
            }
        } catch (e: Exception) {
            FlagValueParseOutcome.Failure(e.message ?: "Invalid JSON")
        }
    }

    private sealed interface RuleUpdateOutcome {
        data class Updated(val rule: SerializableRule) : RuleUpdateOutcome
        data class Rejected(val message: String) : RuleUpdateOutcome
    }

    private fun updateRuleFromParams(
        existing: SerializableRule,
        params: io.ktor.http.Parameters,
        supported: io.amichne.konditional.configstate.SupportedValues,
    ): RuleUpdateOutcome {
        val valueOutcome = parseFlagValueJson(params["valueJson"].orEmpty())
        if (valueOutcome is FlagValueParseOutcome.Failure) {
            return RuleUpdateOutcome.Rejected(valueOutcome.message)
        }

        val rampUpRaw = params["rampUp"].orEmpty().trim()
        val rampUp = rampUpRaw.toDoubleOrNull()
        val rampDescriptor = supported.byType[FieldType.RAMP_UP_PERCENT.name] as? NumberRangeDescriptor
        val rampError =
            if (rampUp == null) {
                "Invalid rampUp"
            } else {
                validateNumber(rampUp, rampDescriptor)
            }
        if (rampError != null) {
            return RuleUpdateOutcome.Rejected(rampError)
        }
        val rampUpValue = requireNotNull(rampUp)

        val noteDescriptor = supported.byType[FieldType.RULE_NOTE.name] as? StringConstraintsDescriptor
        val noteRaw = params["note"].orEmpty().trim()
        val noteError = noteRaw.takeIf(String::isNotBlank)?.let { validateString(it, noteDescriptor) }
        if (noteError != null) {
            return RuleUpdateOutcome.Rejected(noteError)
        }
        val note = noteRaw.ifBlank { null }

        val ruleAllowlistRaw = params["ruleAllowlist"].orEmpty()
        val ruleAllowlist =
            ruleAllowlistRaw.lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet()
        val allowlistDescriptor = supported.byType[FieldType.RAMP_UP_ALLOWLIST.name] as? StringConstraintsDescriptor
        val allowlistError = validateStringSet(ruleAllowlist, allowlistDescriptor)
        if (allowlistError != null) {
            return RuleUpdateOutcome.Rejected(allowlistError)
        }

        val locales = params.getAll("locales").orEmpty().toSet()
        val localesDescriptor = supported.byType[FieldType.LOCALES.name] as? EnumOptionsDescriptor
        val localesError = validateEnumOptions(locales, localesDescriptor)
        if (localesError != null) {
            return RuleUpdateOutcome.Rejected(localesError)
        }

        val platforms = params.getAll("platforms").orEmpty().toSet()
        val platformsDescriptor = supported.byType[FieldType.PLATFORMS.name] as? EnumOptionsDescriptor
        val platformsError = validateEnumOptions(platforms, platformsDescriptor)
        if (platformsError != null) {
            return RuleUpdateOutcome.Rejected(platformsError)
        }

        val axesRaw = params["axes"].orEmpty()
        val axesOutcome = parseAxesMap(axesRaw)
        if (axesOutcome is AxesParseOutcome.Failure) {
            return RuleUpdateOutcome.Rejected(axesOutcome.message)
        }
        val axes = (axesOutcome as AxesParseOutcome.Success).axes
        val axesDescriptor = supported.byType[FieldType.AXES_MAP.name] as? MapConstraintsDescriptor
        val axesError = validateAxesMap(axes, axesDescriptor)
        if (axesError != null) {
            return RuleUpdateOutcome.Rejected(axesError)
        }

        val rangeOutcome = parseVersionRange(params = params, supported = supported)
        if (rangeOutcome is VersionRangeParseOutcome.Failure) {
            return RuleUpdateOutcome.Rejected(rangeOutcome.message)
        }
        val versionRange = (rangeOutcome as VersionRangeParseOutcome.Success).range

        val value = (valueOutcome as FlagValueParseOutcome.Success).value
        val updated =
            existing.copy(
                value = value,
                rampUp = rampUpValue,
                rampUpAllowlist = ruleAllowlist,
                note = note,
                locales = locales,
                platforms = platforms,
                versionRange = versionRange,
                axes = axes,
            )

        return RuleUpdateOutcome.Updated(updated)
    }

    private fun validateNumber(value: Double, descriptor: NumberRangeDescriptor?): String? {
        if (descriptor == null) {
            return null
        }

        val min = descriptor.min
        if (value < min) {
            return "Must be >= $min"
        }

        val max = descriptor.max
        if (value > max) {
            return "Must be <= $max"
        }

        return null
    }

    private fun validateEnumOptions(values: Set<String>, descriptor: EnumOptionsDescriptor?): String? {
        if (descriptor == null) {
            return null
        }

        val allowed = descriptor.options.map { it.value }.toSet()
        val invalid = values.firstOrNull { it !in allowed }
        return invalid?.let { "Invalid selection: $it" }
    }

    private sealed interface AxesParseOutcome {
        data class Success(val axes: Map<String, Set<String>>) : AxesParseOutcome
        data class Failure(val message: String) : AxesParseOutcome
    }

    private fun parseAxesMap(raw: String): AxesParseOutcome {
        val axes = mutableMapOf<String, MutableSet<String>>()
        var error: String? = null

        val lines =
            raw.lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)

        for (line in lines) {
            val idx = line.indexOf('=')
            if (idx <= 0) {
                error = "Invalid axes line: '$line' (expected axisId=value1,value2)"
                break
            }

            val axisId = line.substring(0, idx).trim()
            val values =
                line.substring(idx + 1)
                    .split(',')
                    .asSequence()
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .toSet()

            if (values.isNotEmpty()) {
                axes.getOrPut(axisId) { mutableSetOf() }.addAll(values)
            }
        }

        return error?.let(AxesParseOutcome::Failure)
            ?: AxesParseOutcome.Success(axes.mapValues { (_, set) -> set.toSet() })
    }

    private fun validateAxesMap(axes: Map<String, Set<String>>, descriptor: MapConstraintsDescriptor?): String? {
        if (descriptor == null) {
            return null
        }

        val keyDescriptor = descriptor.key
        val valueDescriptor = descriptor.values

        val invalidKey = axes.keys.firstOrNull { validateString(it, keyDescriptor) != null }
        if (invalidKey != null) {
            return "Invalid axis ID: $invalidKey"
        }

        axes.values.forEach { set ->
            val error = validateStringSet(set, valueDescriptor)
            if (error != null) {
                return error
            }
        }

        return null
    }

    private sealed interface VersionRangeParseOutcome {
        data class Success(val range: VersionRange?) : VersionRangeParseOutcome
        data class Failure(val message: String) : VersionRangeParseOutcome
    }

    private fun parseVersionRange(
        params: io.ktor.http.Parameters,
        supported: io.amichne.konditional.configstate.SupportedValues,
    ): VersionRangeParseOutcome {
        val type = params["versionRangeType"].orEmpty().trim()
        if (type.isBlank() || type == VersionRange.Type.UNBOUNDED.name) {
            return VersionRangeParseOutcome.Success(null)
        }

        val semverDescriptor = supported.byType[FieldType.SEMVER.name] as? SemverConstraintsDescriptor
        val minRaw = params["versionMin"].orEmpty().trim()
        val maxRaw = params["versionMax"].orEmpty().trim()

        fun parseSemver(raw: String): Pair<Version?, String?> {
            if (raw.isBlank()) {
                return null to null
            }

            val parsed = Version.parse(raw)
            val version =
                when (parsed) {
                    is ParseResult.Success -> parsed.value
                    is ParseResult.Failure -> return null to (parsed.error.toString())
                }

            val minimum = semverDescriptor?.minimum?.let { Version.parse(it) }
            if (minimum is ParseResult.Success && version < minimum.value) {
                return null to "Version must be >= ${minimum.value}"
            }

            return version to null
        }

        val (min, minError) = parseSemver(minRaw)
        if (minError != null) {
            return VersionRangeParseOutcome.Failure("Min version: $minError")
        }

        val (max, maxError) = parseSemver(maxRaw)
        if (maxError != null) {
            return VersionRangeParseOutcome.Failure("Max version: $maxError")
        }

        val range =
            when (type) {
                VersionRange.Type.MIN_BOUND.name ->
                    min?.let(::LeftBound) ?: return VersionRangeParseOutcome.Failure("Min version required")
                VersionRange.Type.MAX_BOUND.name ->
                    max?.let(::RightBound) ?: return VersionRangeParseOutcome.Failure("Max version required")
                VersionRange.Type.MIN_AND_MAX_BOUND.name -> {
                    if (min == null || max == null) {
                        return VersionRangeParseOutcome.Failure("Min and max required")
                    }
                    runCatching { FullyBound(min, max) }.getOrElse { return VersionRangeParseOutcome.Failure(it.message ?: "Invalid range") }
                }
                else -> return VersionRangeParseOutcome.Failure("Unknown version range type: $type")
            }

        return VersionRangeParseOutcome.Success(range)
    }

    private fun validateString(value: String, descriptor: StringConstraintsDescriptor?): String? {
        if (descriptor == null) {
            return null
        }

        val minLength = descriptor.minLength
        if (minLength != null && value.length < minLength) {
            return "Must be at least $minLength characters"
        }

        val maxLength = descriptor.maxLength
        if (maxLength != null && value.length > maxLength) {
            return "Must be at most $maxLength characters"
        }

        val pattern = descriptor.pattern
        if (!pattern.isNullOrBlank() && !Regex(pattern).matches(value)) {
            return "Does not match required pattern"
        }

        return null
    }

    private fun validateStringSet(values: Set<String>, descriptor: StringConstraintsDescriptor?): String? {
        if (descriptor == null) {
            return null
        }

        val pattern = descriptor.pattern?.takeIf(String::isNotBlank)?.let(::Regex)
        if (pattern != null) {
            val invalid = values.firstOrNull { !pattern.matches(it) }
            if (invalid != null) {
                return "Invalid value: $invalid"
            }
        }

        val minLength = descriptor.minLength
        if (minLength != null) {
            val invalid = values.firstOrNull { it.length < minLength }
            if (invalid != null) {
                return "Too short: $invalid"
            }
        }

        val maxLength = descriptor.maxLength
        if (maxLength != null) {
            val invalid = values.firstOrNull { it.length > maxLength }
            if (invalid != null) {
                return "Too long: $invalid"
            }
        }

        return null
    }

    private fun updateFlag(
        key: String,
        transform: (SerializableFlag) -> FlagUpdate,
    ): MutationOutcome =
        runCatching { FeatureId.parse(key) }
            .fold(
                onSuccess = { id ->
                    val currentConfig = DemoFeatures.configuration
                    val currentSnapshot = SnapshotSerializer.toSerializableSnapshot(currentConfig)
                    val currentFlag = currentSnapshot.flags.firstOrNull { it.key == id }

                    currentFlag
                        ?.let(transform)
                        ?.let { update ->
                            when (update) {
                                is FlagUpdate.Rejected -> MutationOutcome.Failure(update.message)
                                is FlagUpdate.Updated -> {
                                    val patch =
                                        SerializablePatch(
                                            meta = null,
                                            flags = listOf(update.flag),
                                            removeKeys = emptyList(),
                                        )

                                    val patchJson = patchAdapter.toJson(patch)
                                    when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
                                        is ParseResult.Success -> MutationOutcome.Success(result.value)
                                        is ParseResult.Failure -> MutationOutcome.Failure(result.error.toString())
                                    }
                                }
                            }
                        } ?: MutationOutcome.Failure("Flag not found: $key")
                },
                onFailure = {
                    MutationOutcome.Failure("Invalid FeatureId: $key")
                },
            )
}

private sealed interface FlagValueParseOutcome {
    data class Success(val value: FlagValue<*>) : FlagValueParseOutcome
    data class Failure(val message: String) : FlagValueParseOutcome
}

private sealed interface FlagUpdate {
    data class Updated(val flag: SerializableFlag) : FlagUpdate
    data class Rejected(val message: String) : FlagUpdate
}

private sealed interface MutationOutcome {
    data class Success(val configuration: Configuration) : MutationOutcome
    data class Failure(val message: String) : MutationOutcome
}

private object ConfigPagesFragments {
    fun flagList(filter: String): String {
        val snapshot = ConfigurationStateFactory.from(DemoFeatures.configuration).currentState
        val normalized = filter.trim().lowercase()

        val flags =
            snapshot.flags
                .asSequence()
                .map { flag ->
                    val parts = ConfigPagesIds.parse(flag.key.toString())
                    FlagListItem(
                        key = flag.key.toString(),
                        encodedKey = jsEncodeURIComponent(flag.key.toString()),
                        namespace = parts.namespace,
                        shortKey = parts.key,
                        type = flag.defaultValue.toValueType().name,
                        isActive = flag.isActive,
                        rulesCount = flag.rules.size,
                        defaultSummary = summarize(flag.defaultValue),
                    )
                }.filter { item ->
                    normalized.isBlank() ||
                        item.key.lowercase().contains(normalized) ||
                        item.shortKey.lowercase().contains(normalized) ||
                        item.namespace.lowercase().contains(normalized)
                }.sortedWith(compareBy<FlagListItem> { it.namespace }.thenBy { it.shortKey })
                .toList()

        return createHTML().div {
            id = "configPagesFlagList"
            div { +"Flags (${flags.size})" }

            flags.forEach { item ->
                div(classes = "configPages-flagRow") {
                    attributes["hx-get"] = "/config-pages/fragments/flag-detail?key=${item.encodedKey}"
                    attributes["hx-target"] = "#configPagesFlagDetail"
                    attributes["hx-swap"] = "outerHTML"
                    attributes["hx-include"] = "#configPagesFilter"
                    attributes["role"] = "button"
                    attributes["tabindex"] = "0"

                    div(classes = "configPages-flagTitle") {
                        +item.shortKey
                        if (!item.isActive) {
                            span(classes = "configPages-pill") { +"Inactive" }
                        }
                    }
                    div(classes = "configPages-flagMeta") {
                        +("${item.namespace} • ${item.type} • ${item.rulesCount} rule(s) • default=${item.defaultSummary}")
                    }
                }
            }
        }
    }

    fun flagDetail(
        flag: SerializableFlag,
        supported: io.amichne.konditional.configstate.SupportedValues,
        filter: String,
        error: String?,
    ): String {
        val key = flag.key.toString()
        val parts = ConfigPagesIds.parse(key)
        val defaultEditor = defaultValueEditor(flag.defaultValue, supported)

        return createHTML().div {
            id = "configPagesFlagDetail"

            div(classes = "configPages-card") {
                div(classes = "configPages-header") {
                    div {
                        div(classes = "configPages-title") { +parts.key }
                        div(classes = "configPages-muted") { +key }
                    }
                }

                if (!error.isNullOrBlank()) {
                    div(classes = "configPages-error") { +error }
                }

                renderBooleanField(
                    supported = supported,
                    fieldType = FieldType.FLAG_ACTIVE,
                    action = "/config-pages/actions/flag/active",
                    key = key,
                    filter = filter,
                    name = "isActive",
                    value = flag.isActive,
                )

                div(classes = "configPages-section") {
                    val ui = fieldUi(supported, FieldType.FLAG_VALUE)
                    div(classes = "configPages-sectionTitle") { +(ui.label ?: "Value") }
                    ui.helpText?.let { div(classes = "configPages-muted") { +it } }
                    form(classes = "configPages-inlineForm") {
                        attributes["hx-post"] = "/config-pages/actions/flag/default"
                        attributes["hx-target"] = "#configPagesFlagDetail"
                        attributes["hx-swap"] = "outerHTML"
                        attributes["hx-include"] = "#configPagesFilter"

                        input(type = kotlinx.html.InputType.hidden) {
                            name = "key"
                            value = key
                        }
                        input(type = kotlinx.html.InputType.hidden) {
                            name = "filter"
                            value = filter
                        }

                        unsafe {
                            raw(defaultEditor)
                        }

                        button(classes = "configPages-button") {
                            type = kotlinx.html.ButtonType.submit
                            +"Save"
                        }
                    }
                }

                div(classes = "configPages-section") {
                    val ui = fieldUi(supported, FieldType.SALT)
                    div(classes = "configPages-sectionTitle") { +(ui.label ?: "Salt") }
                    ui.helpText?.let { div(classes = "configPages-muted") { +it } }

                    form(classes = "configPages-inlineForm") {
                        attributes["hx-post"] = "/config-pages/actions/flag/salt"
                        attributes["hx-target"] = "#configPagesFlagDetail"
                        attributes["hx-swap"] = "outerHTML"
                        attributes["hx-include"] = "#configPagesFilter"

                        input(type = InputType.hidden) {
                            name = "key"
                            value = key
                        }
                        input(type = InputType.hidden) {
                            name = "filter"
                            value = filter
                        }

                        val constraints = supported.byType[FieldType.SALT.name] as? StringConstraintsDescriptor
                        input(type = InputType.text) {
                            name = "salt"
                            value = flag.salt
                            ui.placeholder?.let { placeholder = it }
                            constraints?.pattern?.let { attributes["pattern"] = it }
                            constraints?.minLength?.let { attributes["minlength"] = it.toString() }
                            constraints?.maxLength?.let { attributes["maxlength"] = it.toString() }
                        }

                        button(classes = "configPages-button") {
                            type = kotlinx.html.ButtonType.submit
                            +"Save"
                        }
                    }
                }

                div(classes = "configPages-section") {
                    val ui = fieldUi(supported, FieldType.RAMP_UP_ALLOWLIST)
                    div(classes = "configPages-sectionTitle") { +(ui.label ?: "Ramp-up allowlist") }
                    ui.helpText?.let { div(classes = "configPages-muted") { +it } }

                    form(classes = "configPages-inlineForm") {
                        attributes["hx-post"] = "/config-pages/actions/flag/allowlist"
                        attributes["hx-target"] = "#configPagesFlagDetail"
                        attributes["hx-swap"] = "outerHTML"
                        attributes["hx-include"] = "#configPagesFilter"

                        input(type = InputType.hidden) {
                            name = "key"
                            value = key
                        }
                        input(type = InputType.hidden) {
                            name = "filter"
                            value = filter
                        }

                        val rawAllowlist = flag.rampUpAllowlist.sorted().joinToString(separator = "\n")
                        textArea {
                            name = "allowlist"
                            attributes["rows"] = "6"
                            +rawAllowlist
                        }

                        button(classes = "configPages-button") {
                            type = kotlinx.html.ButtonType.submit
                            +"Save"
                        }
                    }
                }

                div(classes = "configPages-section") {
                    div(classes = "configPages-sectionTitle") { +"Rules (${flag.rules.size})" }
                    div(classes = "configPages-muted") { +"Edits are validated against SupportedValues; updates swap only the detail fragment." }

                    form(classes = "configPages-inlineForm") {
                        attributes["hx-post"] = "/config-pages/actions/rule/add"
                        attributes["hx-target"] = "#configPagesFlagDetail"
                        attributes["hx-swap"] = "outerHTML"
                        attributes["hx-include"] = "#configPagesFilter"

                        input(type = InputType.hidden) {
                            name = "key"
                            value = key
                        }
                        input(type = InputType.hidden) {
                            name = "filter"
                            value = filter
                        }

                        button(classes = "configPages-button") {
                            type = kotlinx.html.ButtonType.submit
                            +"Add rule"
                        }
                    }

                    if (flag.rules.isEmpty()) {
                        div(classes = "configPages-muted") { +"No rules (default applies to all contexts)." }
                    }

                    flag.rules.forEachIndexed { index, rule ->
                        div(classes = "configPages-ruleCard") {
                            div(classes = "configPages-ruleTitle") {
                                +("Rule ${index + 1}")

                                form {
                                    attributes["style"] = "display:inline-block; margin-left: 10px;"
                                    attributes["hx-post"] = "/config-pages/actions/rule/remove"
                                    attributes["hx-target"] = "#configPagesFlagDetail"
                                    attributes["hx-swap"] = "outerHTML"
                                    attributes["hx-include"] = "#configPagesFilter"

                                    input(type = InputType.hidden) {
                                        name = "key"
                                        value = key
                                    }
                                    input(type = InputType.hidden) {
                                        name = "filter"
                                        value = filter
                                    }
                                    input(type = InputType.hidden) {
                                        name = "index"
                                        value = index.toString()
                                    }

                                    button(classes = "configPages-button") {
                                        type = kotlinx.html.ButtonType.submit
                                        +"Remove"
                                    }
                                }
                            }

                            form(classes = "configPages-inlineForm") {
                                attributes["hx-post"] = "/config-pages/actions/rule/update"
                                attributes["hx-target"] = "#configPagesFlagDetail"
                                attributes["hx-swap"] = "outerHTML"
                                attributes["hx-include"] = "#configPagesFilter"

                                input(type = InputType.hidden) {
                                    name = "key"
                                    value = key
                                }
                                input(type = InputType.hidden) {
                                    name = "filter"
                                    value = filter
                                }
                                input(type = InputType.hidden) {
                                    name = "index"
                                    value = index.toString()
                                }

                                val rampUi = fieldUi(supported, FieldType.RAMP_UP_PERCENT)
                                val rampDescriptor = supported.byType[FieldType.RAMP_UP_PERCENT.name] as? NumberRangeDescriptor
                                div(classes = "configPages-field") {
                                    span { +(rampUi.label ?: "Ramp-up") }
                                    input(type = InputType.number) {
                                        name = "rampUp"
                                        value = rule.rampUp.toString()
                                        rampDescriptor?.min?.let { attributes["min"] = it.toString() }
                                        rampDescriptor?.max?.let { attributes["max"] = it.toString() }
                                        rampDescriptor?.step?.let { attributes["step"] = it.toString() }
                                    }
                                    rampUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val valueUi = fieldUi(supported, FieldType.FLAG_VALUE)
                                val valueJson =
                                    SnapshotSerializer
                                        .defaultMoshi()
                                        .adapter(FlagValue::class.java)
                                        .indent("  ")
                                        .toJson(rule.value)
                                div(classes = "configPages-field") {
                                    span { +(valueUi.label ?: "Value") }
                                    textArea {
                                        name = "valueJson"
                                        attributes["rows"] = "8"
                                        +valueJson
                                    }
                                    valueUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val allowlistUi = fieldUi(supported, FieldType.RAMP_UP_ALLOWLIST)
                                val allowlistRaw = rule.rampUpAllowlist.sorted().joinToString(separator = "\n")
                                div(classes = "configPages-field") {
                                    span { +(allowlistUi.label ?: "Ramp-up allowlist") }
                                    textArea {
                                        name = "ruleAllowlist"
                                        attributes["rows"] = "5"
                                        +allowlistRaw
                                    }
                                    allowlistUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val noteUi = fieldUi(supported, FieldType.RULE_NOTE)
                                div(classes = "configPages-field") {
                                    span { +(noteUi.label ?: "Note") }
                                    textArea {
                                        name = "note"
                                        attributes["rows"] = "3"
                                        +(rule.note.orEmpty())
                                    }
                                    noteUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val localesDescriptor = supported.byType[FieldType.LOCALES.name] as? EnumOptionsDescriptor
                                val localesUi = fieldUi(supported, FieldType.LOCALES)
                                div(classes = "configPages-field") {
                                    span { +(localesUi.label ?: "Locales") }
                                    select {
                                        name = "locales"
                                        attributes["multiple"] = "multiple"
                                        localesDescriptor?.options?.forEach { option ->
                                            option {
                                                value = option.value
                                                selected = option.value in rule.locales
                                                +option.label
                                            }
                                        }
                                    }
                                    localesUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val platformsDescriptor = supported.byType[FieldType.PLATFORMS.name] as? EnumOptionsDescriptor
                                val platformsUi = fieldUi(supported, FieldType.PLATFORMS)
                                div(classes = "configPages-field") {
                                    span { +(platformsUi.label ?: "Platforms") }
                                    select {
                                        name = "platforms"
                                        attributes["multiple"] = "multiple"
                                        platformsDescriptor?.options?.forEach { option ->
                                            option {
                                                value = option.value
                                                selected = option.value in rule.platforms
                                                +option.label
                                            }
                                        }
                                    }
                                    platformsUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val rangeUi = fieldUi(supported, FieldType.VERSION_RANGE)
                                val range = rule.versionRange
                                val rangeMin =
                                    if (range != null && (range.type == VersionRange.Type.MIN_BOUND || range.type == VersionRange.Type.MIN_AND_MAX_BOUND)) {
                                        range.min?.let(::formatVersion).orEmpty()
                                    } else {
                                        ""
                                    }
                                val rangeMax =
                                    if (range != null && (range.type == VersionRange.Type.MAX_BOUND || range.type == VersionRange.Type.MIN_AND_MAX_BOUND)) {
                                        range.max?.let(::formatVersion).orEmpty()
                                    } else {
                                        ""
                                    }
                                div(classes = "configPages-field") {
                                    span { +(rangeUi.label ?: "Version range") }
                                    select {
                                        name = "versionRangeType"
                                        option {
                                            value = ""
                                            selected = range == null || range.type == VersionRange.Type.UNBOUNDED
                                            +"(no version targeting)"
                                        }
                                        option {
                                            value = VersionRange.Type.MIN_BOUND.name
                                            selected = range?.type == VersionRange.Type.MIN_BOUND
                                            +"Min bound"
                                        }
                                        option {
                                            value = VersionRange.Type.MAX_BOUND.name
                                            selected = range?.type == VersionRange.Type.MAX_BOUND
                                            +"Max bound"
                                        }
                                        option {
                                            value = VersionRange.Type.MIN_AND_MAX_BOUND.name
                                            selected = range?.type == VersionRange.Type.MIN_AND_MAX_BOUND
                                            +"Min and max bound"
                                        }
                                    }
                                    input(type = InputType.text) {
                                        name = "versionMin"
                                        value = rangeMin
                                        placeholder = "1.2.3"
                                    }
                                    input(type = InputType.text) {
                                        name = "versionMax"
                                        value = rangeMax
                                        placeholder = "2.0.0"
                                    }
                                    rangeUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                val axesUi = fieldUi(supported, FieldType.AXES_MAP)
                                val axesRaw =
                                    rule.axes.entries
                                        .sortedBy { it.key }
                                        .joinToString(separator = "\n") { (k, v) -> "$k=${v.sorted().joinToString(separator = ",")}" }
                                div(classes = "configPages-field") {
                                    span { +(axesUi.label ?: "Axes") }
                                    textArea {
                                        name = "axes"
                                        attributes["rows"] = "5"
                                        +axesRaw
                                    }
                                    axesUi.helpText?.let { div(classes = "configPages-muted") { +it } }
                                }

                                button(classes = "configPages-button") {
                                    type = kotlinx.html.ButtonType.submit
                                    +"Save"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun flagDetailNotFound(key: String): String =
        createHTML().div {
            id = "configPagesFlagDetail"
            div(classes = "configPages-card") {
                div(classes = "configPages-title") { +"Not found" }
                div(classes = "configPages-muted") { +key }
            }
        }

    fun toast(message: String?, oob: Boolean): String =
        createHTML().div {
            id = "configPagesToast"
            if (oob) {
                attributes["hx-swap-oob"] = "outerHTML"
            }
            if (message.isNullOrBlank()) {
                attributes["class"] = "configPages-toast configPages-toastHidden"
            } else {
                attributes["class"] = "configPages-toast"
                +message
                attributes["hx-get"] = "/config-pages/fragments/toast/clear"
                attributes["hx-trigger"] = "load delay:2200ms"
                attributes["hx-swap"] = "outerHTML"
            }
        }

    private fun defaultValueEditor(value: FlagValue<*>, supported: io.amichne.konditional.configstate.SupportedValues): String {
        val moshi = SnapshotSerializer.defaultMoshi()

        val adapter = moshi.adapter(FlagValue::class.java).indent("  ")
        val json = adapter.toJson(value)
        val ui = fieldUi(supported, FieldType.FLAG_VALUE)

        return """
            <label class="configPages-field">
              <span>${htmlEscape(ui.label ?: "Value (JSON)")}</span>
              <textarea name="valueJson" rows="10">${htmlEscape(json)}</textarea>
              ${ui.helpText?.let { "<small class=\"configPages-muted\">${htmlEscape(it)}</small>" }.orEmpty()}
            </label>
        """.trimIndent()
    }

    private fun summarize(value: FlagValue<*>): String =
        SnapshotSerializer
            .defaultMoshi()
            .adapter(FlagValue::class.java)
            .toJson(value)
            .replace("\n", "")
            .take(64)

    private fun htmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}

private data class FlagListItem(
    val key: String,
    val encodedKey: String,
    val namespace: String,
    val shortKey: String,
    val type: String,
    val isActive: Boolean,
    val rulesCount: Int,
    val defaultSummary: String,
)

private object ConfigPagesIds {
    private val featureIdRegex = Regex("""^(feature|value)::([^:]+)::(.+)$""")

    fun parse(featureId: String): ParsedFeatureId {
        val match = featureIdRegex.find(featureId)
        val parts =
            if (match != null) {
                ParsedFeatureId(
                    namespace = match.groupValues[2],
                    key = match.groupValues[3],
                )
            } else {
                ParsedFeatureId(namespace = "unknown", key = featureId)
            }
        return parts
    }
}

private data class ParsedFeatureId(
    val namespace: String,
    val key: String,
)

private fun jsEncodeURIComponent(value: String): String =
    java.net.URLEncoder.encode(value, UTF_8)

private fun formatVersion(version: Version): String = "${version.major}.${version.minor}.${version.patch}"

private data class FieldUi(
    val label: String?,
    val helpText: String?,
    val placeholder: String?,
)

private fun fieldUi(supported: io.amichne.konditional.configstate.SupportedValues, fieldType: FieldType): FieldUi {
    val descriptor = supported.byType[fieldType.name]
    val uiHints = descriptor?.uiHints
    return FieldUi(
        label = uiHints?.label,
        helpText = uiHints?.helpText,
        placeholder = uiHints?.placeholder,
    )
}

private fun kotlinx.html.FlowContent.renderBooleanField(
    supported: io.amichne.konditional.configstate.SupportedValues,
    fieldType: FieldType,
    action: String,
    key: String,
    filter: String,
    name: String,
    value: Boolean,
) {
    val ui = fieldUi(supported, fieldType)
    div(classes = "configPages-section") {
        div(classes = "configPages-sectionTitle") { +(ui.label ?: fieldType.name) }
        ui.helpText?.let { div(classes = "configPages-muted") { +it } }
        form(classes = "configPages-inlineForm") {
            attributes["hx-post"] = action
            attributes["hx-trigger"] = "change"
            attributes["hx-target"] = "#configPagesFlagDetail"
            attributes["hx-swap"] = "outerHTML"
            attributes["hx-include"] = "#configPagesFilter"

            input(type = InputType.hidden) {
                this.name = "key"
                this.value = key
            }
            input(type = InputType.hidden) {
                this.name = "filter"
                this.value = filter
            }

            select {
                this.name = name
                option {
                    this.value = "true"
                    selected = value
                    +"true"
                }
                option {
                    this.value = "false"
                    selected = !value
                    +"false"
                }
            }
        }
    }
}

private fun HTML.renderConfigPagesShell() {
    head {
        title { +"Konditional Demo - Config Pages" }
        style {
            unsafe {
                raw(
                    """
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                      background: #0b1220;
                      color: #e5e7eb;
                      padding: 18px;
                    }
                    a { color: inherit; }
                    .configPages-shell {
                      max-width: 1400px;
                      margin: 0 auto;
                      border: 1px solid rgba(255,255,255,0.08);
                      border-radius: 14px;
                      background: rgba(255,255,255,0.03);
                      overflow: hidden;
                      box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
                    }
                    .configPages-top {
                      display: flex;
                      gap: 12px;
                      align-items: center;
                      padding: 12px 14px;
                      border-bottom: 1px solid rgba(255,255,255,0.08);
                      background: rgba(255,255,255,0.02);
                    }
                    .configPages-input {
                      width: 420px;
                      max-width: 100%;
                      border-radius: 10px;
                      border: 1px solid rgba(255,255,255,0.10);
                      background: rgba(255,255,255,0.06);
                      color: inherit;
                      padding: 10px 12px;
                      outline: none;
                    }
                    .configPages-grid {
                      display: grid;
                      grid-template-columns: 420px 1fr;
                      gap: 14px;
                      padding: 14px;
                      align-items: start;
                    }
                    .configPages-card {
                      padding: 14px;
                      border-radius: 12px;
                      border: 1px solid rgba(255,255,255,0.10);
                      background: rgba(255,255,255,0.04);
                    }
                    .configPages-title { font-weight: 800; font-size: 18px; }
                    .configPages-muted { opacity: 0.75; font-size: 12px; }
                    .configPages-flagRow {
                      margin-top: 10px;
                      padding: 10px;
                      border-radius: 12px;
                      border: 1px solid rgba(255,255,255,0.08);
                      background: rgba(255,255,255,0.03);
                      cursor: pointer;
                    }
                    .configPages-flagRow:hover { background: rgba(255,255,255,0.06); }
                    .configPages-flagTitle { display: flex; gap: 8px; align-items: center; font-weight: 700; }
                    .configPages-flagMeta { opacity: 0.75; font-size: 12px; margin-top: 4px; }
                    .configPages-pill {
                      padding: 1px 8px;
                      border-radius: 999px;
                      border: 1px solid rgba(255,255,255,0.12);
                      background: rgba(255,255,255,0.06);
                      font-size: 12px;
                      opacity: 0.85;
                    }
                    .configPages-header { display: flex; justify-content: space-between; gap: 12px; align-items: start; }
                    .configPages-section { margin-top: 14px; }
                    .configPages-sectionTitle { font-weight: 700; margin-bottom: 8px; }
                    .configPages-inlineForm { display: flex; gap: 10px; align-items: end; flex-wrap: wrap; }
                    .configPages-field { display: grid; gap: 6px; min-width: 280px; }
                    .configPages-field input, .configPages-field select, .configPages-field textarea {
                      border-radius: 10px;
                      border: 1px solid rgba(255,255,255,0.10);
                      background: rgba(255,255,255,0.06);
                      color: inherit;
                      padding: 10px 12px;
                      outline: none;
                    }
                    .configPages-button {
                      border-radius: 10px;
                      border: 1px solid rgba(255,255,255,0.16);
                      background: rgba(255,255,255,0.08);
                      color: inherit;
                      padding: 10px 12px;
                      cursor: pointer;
                      font-weight: 700;
                    }
                    .configPages-button:hover { background: rgba(255,255,255,0.12); }
                    .configPages-ruleCard {
                      margin-top: 10px;
                      padding: 10px;
                      border-radius: 12px;
                      border: 1px solid rgba(255,255,255,0.08);
                      background: rgba(255,255,255,0.03);
                    }
                    .configPages-ruleTitle { font-weight: 700; margin-bottom: 4px; }
                    .configPages-error {
                      margin-top: 10px;
                      padding: 10px;
                      border-radius: 12px;
                      border: 1px solid rgba(255,255,255,0.18);
                      background: rgba(239, 68, 68, 0.16);
                    }
                    .configPages-toast {
                      position: fixed;
                      right: 18px;
                      bottom: 18px;
                      max-width: 420px;
                      padding: 10px 12px;
                      border-radius: 12px;
                      border: 1px solid rgba(255,255,255,0.12);
                      background: rgba(15, 23, 42, 0.96);
                    }
                    .configPages-toastHidden { display: none; }
                    """.trimIndent(),
                )
            }
        }
    }
    body {
        div(classes = "configPages-shell") {
            div(classes = "configPages-top") {
                div(classes = "configPages-title") { +"Config Pages" }
                input(type = kotlinx.html.InputType.search, classes = "configPages-input") {
                    id = "configPagesFilter"
                    name = "filter"
                    placeholder = "Filter flags…"
                    attributes["hx-get"] = "/config-pages/fragments/flag-list"
                    attributes["hx-trigger"] = "input changed delay:200ms"
                    attributes["hx-target"] = "#configPagesFlagList"
                    attributes["hx-swap"] = "outerHTML"
                }
                div(classes = "configPages-muted") { +"(Rendered from SerializableSnapshot + SupportedValues)" }
            }

            div(classes = "configPages-grid") {
                div {
                    attributes["hx-get"] = "/config-pages/fragments/flag-list"
                    attributes["hx-trigger"] = "load, configPagesUpdated from:body"
                    attributes["hx-target"] = "#configPagesFlagList"
                    attributes["hx-swap"] = "outerHTML"
                    attributes["hx-include"] = "#configPagesFilter"
                    div {
                        id = "configPagesFlagList"
                        div(classes = "configPages-card") { +"Loading…" }
                    }
                }

                div {
                    id = "configPagesFlagDetail"
                    div(classes = "configPages-card") {
                        div(classes = "configPages-title") { +"Select a flag" }
                        div(classes = "configPages-muted") { +"Click a flag to view and edit its default value." }
                    }
                }
            }
        }

        div {
            id = "configPagesToast"
            attributes["class"] = "configPages-toast configPages-toastHidden"
        }

        script {
            src = "/static/htmx.min.js"
        }
    }
}
