package io.amichne.konditional.uispec.konditional

import io.amichne.konditional.uispec.JsonPointer
import io.amichne.konditional.uispec.UiCollection
import io.amichne.konditional.uispec.UiCondition
import io.amichne.konditional.uispec.UiConditionGroup
import io.amichne.konditional.uispec.UiConditionOperator
import io.amichne.konditional.uispec.UiControlType
import io.amichne.konditional.uispec.UiDensity
import io.amichne.konditional.uispec.UiEmptyState
import io.amichne.konditional.uispec.UiEnum
import io.amichne.konditional.uispec.UiField
import io.amichne.konditional.uispec.UiGroup
import io.amichne.konditional.uispec.UiInputHints
import io.amichne.konditional.uispec.UiLayout
import io.amichne.konditional.uispec.UiLayoutDirection
import io.amichne.konditional.uispec.UiNode
import io.amichne.konditional.uispec.UiNodeId
import io.amichne.konditional.uispec.UiNodeMeta
import io.amichne.konditional.uispec.UiOption
import io.amichne.konditional.uispec.UiPage
import io.amichne.konditional.uispec.UiSection
import io.amichne.konditional.uispec.UiSpec
import io.amichne.konditional.uispec.UiSpecMetadata
import io.amichne.konditional.uispec.UiSpacing
import io.amichne.konditional.uispec.UiText
import io.amichne.konditional.uispec.UiTextLiteral
import io.amichne.konditional.uispec.UiValueKind

private const val specVersion = "v1"

fun konditionalUiSpec(): UiSpec =
    UiSpec(
        version = specVersion,
        metadata = UiSpecMetadata(
            title = text("Konditional Config"),
            description = text("Declarative configuration UI for flags and targeting rules."),
            tags = listOf("konditional", "flags", "config"),
        ),
        root = UiPage(
            id = UiNodeId("konditional.config.page"),
            meta = UiNodeMeta(
                title = text("Konditional"),
                description = text("Edit flags, defaults, and targeting rules."),
            ),
            layout = UiLayout(
                direction = UiLayoutDirection.VERTICAL,
                spacing = UiSpacing.LG,
                density = UiDensity.COMFORTABLE,
            ),
            children = listOf(
                flagsSection(),
            ),
        ),
    )

private fun flagsSection(): UiSection =
    UiSection(
        id = UiNodeId("konditional.flags.section"),
        meta = UiNodeMeta(
            title = text("Flags"),
            description = text("Manage flag defaults and rules."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.LG,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            flagsCollection(),
        ),
    )

private fun flagsCollection(): UiCollection =
    UiCollection(
        id = UiNodeId("konditional.flags.collection"),
        meta = UiNodeMeta(
            title = text("Flags"),
            description = text("Each item represents a feature flag definition."),
        ),
        itemsPointer = JsonPointer("/flags"),
        itemKeyPointer = JsonPointer("/flags/*/key"),
        itemTemplate = flagTemplate(),
        emptyState = UiEmptyState(
            title = text("No flags"),
            description = text("Add a flag to start configuring your application."),
            actions = emptyList(),
        ),
    )

private fun flagTemplate(): UiNode =
    UiSection(
        id = UiNodeId("konditional.flag"),
        meta = UiNodeMeta(
            title = text("Flag"),
            description = text("Flag defaults, metadata, and rules."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.MD,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            flagOverviewGroup(),
            flagDefaultValueSection(),
            flagRulesSection(),
        ),
    )

private fun flagOverviewGroup(): UiGroup =
    UiGroup(
        id = UiNodeId("konditional.flag.overview"),
        meta = UiNodeMeta(
            title = text("Overview"),
            description = text("Primary identity and status for the flag."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.HORIZONTAL,
            spacing = UiSpacing.MD,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            UiField(
                id = UiNodeId("konditional.flag.key"),
                meta = UiNodeMeta(
                    title = text("Key"),
                    description = text("Stable identifier for the flag."),
                ),
                target = JsonPointer("/flags/*/key"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                readOnly = true,
                placeholder = text("feature::<namespace>::<key>"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.active"),
                meta = UiNodeMeta(
                    title = text("Active"),
                    description = text("Disable to force the default value."),
                ),
                target = JsonPointer("/flags/*/isActive"),
                valueKind = UiValueKind.BOOLEAN,
                control = UiControlType.TOGGLE,
            ),
            UiField(
                id = UiNodeId("konditional.flag.salt"),
                meta = UiNodeMeta(
                    title = text("Salt"),
                    description = text("Used for stable bucketing."),
                ),
                target = JsonPointer("/flags/*/salt"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                placeholder = text("v1"),
            ),
        ),
    )

private fun flagDefaultValueSection(): UiSection =
    UiSection(
        id = UiNodeId("konditional.flag.default"),
        meta = UiNodeMeta(
            title = text("Default Value"),
            description = text("Fallback value when no rules match."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.SM,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            UiField(
                id = UiNodeId("konditional.flag.default.type"),
                meta = UiNodeMeta(
                    title = text("Value type"),
                    description = text("Select the default value type."),
                ),
                target = JsonPointer("/flags/*/defaultValue/type"),
                valueKind = UiValueKind.ENUM,
                control = UiControlType.SELECT,
                options = flagValueTypeOptions(),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.boolean"),
                meta = UiNodeMeta(title = text("Boolean value")),
                target = JsonPointer("/flags/*/defaultValue/value"),
                valueKind = UiValueKind.BOOLEAN,
                control = UiControlType.TOGGLE,
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "BOOLEAN"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.string"),
                meta = UiNodeMeta(title = text("String value")),
                target = JsonPointer("/flags/*/defaultValue/value"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "STRING"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.int"),
                meta = UiNodeMeta(title = text("Integer value")),
                target = JsonPointer("/flags/*/defaultValue/value"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "INT"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.double"),
                meta = UiNodeMeta(title = text("Decimal value")),
                target = JsonPointer("/flags/*/defaultValue/value"),
                valueKind = UiValueKind.DOUBLE,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(step = 0.01),
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "DOUBLE"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.enum"),
                meta = UiNodeMeta(title = text("Enum value")),
                target = JsonPointer("/flags/*/defaultValue/value"),
                valueKind = UiValueKind.ENUM,
                control = UiControlType.SELECT,
                options = emptyList(),
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "ENUM"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.enumClass"),
                meta = UiNodeMeta(title = text("Enum class")),
                target = JsonPointer("/flags/*/defaultValue/enumClassName"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "ENUM"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.dataClass"),
                meta = UiNodeMeta(title = text("Data class")),
                target = JsonPointer("/flags/*/defaultValue/dataClassName"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "DATA_CLASS"),
            ),
            UiField(
                id = UiNodeId("konditional.flag.default.data"),
                meta = UiNodeMeta(title = text("Object value")),
                target = JsonPointer("/flags/*/defaultValue/value"),
                valueKind = UiValueKind.JSON,
                control = UiControlType.JSON,
                placeholder = text("{ }"),
                visibility = typeCondition(JsonPointer("/flags/*/defaultValue/type"), "DATA_CLASS"),
            ),
        ),
    )

private fun flagRulesSection(): UiSection =
    UiSection(
        id = UiNodeId("konditional.flag.rules"),
        meta = UiNodeMeta(
            title = text("Rules"),
            description = text("Override defaults for specific targeting."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.MD,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            rulesCollection(),
        ),
    )

private fun rulesCollection(): UiCollection =
    UiCollection(
        id = UiNodeId("konditional.rules.collection"),
        meta = UiNodeMeta(
            title = text("Targeting rules"),
            description = text("Each rule overrides the default value when it matches."),
        ),
        itemsPointer = JsonPointer("/flags/*/rules"),
        itemTemplate = ruleTemplate(),
        emptyState = UiEmptyState(
            title = text("No rules"),
            description = text("Rules are optional; defaults apply when empty."),
            actions = emptyList(),
        ),
    )

private fun ruleTemplate(): UiNode =
    UiSection(
        id = UiNodeId("konditional.rule"),
        meta = UiNodeMeta(
            title = text("Rule"),
            description = text("Targeting, ramp-up, and override value."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.MD,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            ruleValueGroup(),
            ruleTargetingGroup(),
        ),
    )

private fun ruleValueGroup(): UiGroup =
    UiGroup(
        id = UiNodeId("konditional.rule.value"),
        meta = UiNodeMeta(
            title = text("Rule value"),
            description = text("Value used when this rule matches."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.SM,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            UiField(
                id = UiNodeId("konditional.rule.value.type"),
                meta = UiNodeMeta(title = text("Value type")),
                target = JsonPointer("/flags/*/rules/*/value/type"),
                valueKind = UiValueKind.ENUM,
                control = UiControlType.SELECT,
                options = flagValueTypeOptions(),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.boolean"),
                meta = UiNodeMeta(title = text("Boolean value")),
                target = JsonPointer("/flags/*/rules/*/value/value"),
                valueKind = UiValueKind.BOOLEAN,
                control = UiControlType.TOGGLE,
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "BOOLEAN"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.string"),
                meta = UiNodeMeta(title = text("String value")),
                target = JsonPointer("/flags/*/rules/*/value/value"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "STRING"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.int"),
                meta = UiNodeMeta(title = text("Integer value")),
                target = JsonPointer("/flags/*/rules/*/value/value"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "INT"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.double"),
                meta = UiNodeMeta(title = text("Decimal value")),
                target = JsonPointer("/flags/*/rules/*/value/value"),
                valueKind = UiValueKind.DOUBLE,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(step = 0.01),
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "DOUBLE"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.enum"),
                meta = UiNodeMeta(title = text("Enum value")),
                target = JsonPointer("/flags/*/rules/*/value/value"),
                valueKind = UiValueKind.ENUM,
                control = UiControlType.SELECT,
                options = emptyList(),
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "ENUM"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.enumClass"),
                meta = UiNodeMeta(title = text("Enum class")),
                target = JsonPointer("/flags/*/rules/*/value/enumClassName"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "ENUM"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.dataClass"),
                meta = UiNodeMeta(title = text("Data class")),
                target = JsonPointer("/flags/*/rules/*/value/dataClassName"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXT,
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "DATA_CLASS"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.value.data"),
                meta = UiNodeMeta(title = text("Object value")),
                target = JsonPointer("/flags/*/rules/*/value/value"),
                valueKind = UiValueKind.JSON,
                control = UiControlType.JSON,
                placeholder = text("{ }"),
                visibility = typeCondition(JsonPointer("/flags/*/rules/*/value/type"), "DATA_CLASS"),
            ),
        ),
    )

private fun ruleTargetingGroup(): UiGroup =
    UiGroup(
        id = UiNodeId("konditional.rule.targeting"),
        meta = UiNodeMeta(
            title = text("Targeting"),
            description = text("Select where this rule applies."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.SM,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            UiField(
                id = UiNodeId("konditional.rule.note"),
                meta = UiNodeMeta(title = text("Note")),
                target = JsonPointer("/flags/*/rules/*/note"),
                valueKind = UiValueKind.STRING,
                control = UiControlType.TEXTAREA,
                inputHints = UiInputHints(rows = 3),
            ),
            UiField(
                id = UiNodeId("konditional.rule.rampUp"),
                meta = UiNodeMeta(title = text("Ramp-up")),
                target = JsonPointer("/flags/*/rules/*/rampUp"),
                valueKind = UiValueKind.INT,
                control = UiControlType.PERCENT,
                inputHints = UiInputHints(min = 0.0, max = 100.0, step = 1.0),
            ),
            UiField(
                id = UiNodeId("konditional.rule.allowlist"),
                meta = UiNodeMeta(
                    title = text("Ramp-up allowlist"),
                    description = text("Explicit IDs included in the rollout."),
                ),
                target = JsonPointer("/flags/*/rules/*/rampUpAllowlist"),
                valueKind = UiValueKind.ARRAY,
                control = UiControlType.JSON,
                placeholder = text("[]"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.locales"),
                meta = UiNodeMeta(title = text("Locales")),
                target = JsonPointer("/flags/*/rules/*/locales"),
                valueKind = UiValueKind.ARRAY,
                control = UiControlType.MULTISELECT,
                options = localeOptions(),
            ),
            UiField(
                id = UiNodeId("konditional.rule.platforms"),
                meta = UiNodeMeta(title = text("Platforms")),
                target = JsonPointer("/flags/*/rules/*/platforms"),
                valueKind = UiValueKind.ARRAY,
                control = UiControlType.MULTISELECT,
                options = platformOptions(),
            ),
            ruleVersionRangeGroup(),
            UiField(
                id = UiNodeId("konditional.rule.axes"),
                meta = UiNodeMeta(
                    title = text("Axes"),
                    description = text("Custom targeting dimensions."),
                ),
                target = JsonPointer("/flags/*/rules/*/axes"),
                valueKind = UiValueKind.MAP,
                control = UiControlType.KEY_VALUE,
            ),
        ),
    )

private fun ruleVersionRangeGroup(): UiGroup =
    UiGroup(
        id = UiNodeId("konditional.rule.versions"),
        meta = UiNodeMeta(
            title = text("Version range"),
            description = text("Limit the rule to a client version range."),
        ),
        layout = UiLayout(
            direction = UiLayoutDirection.VERTICAL,
            spacing = UiSpacing.SM,
            density = UiDensity.COMFORTABLE,
        ),
        children = listOf(
            UiField(
                id = UiNodeId("konditional.rule.version.type"),
                meta = UiNodeMeta(title = text("Range type")),
                target = JsonPointer("/flags/*/rules/*/versionRange/type"),
                valueKind = UiValueKind.ENUM,
                control = UiControlType.SELECT,
                options = versionRangeOptions(),
            ),
            UiField(
                id = UiNodeId("konditional.rule.version.min.major"),
                meta = UiNodeMeta(title = text("Minimum major")),
                target = JsonPointer("/flags/*/rules/*/versionRange/min/major"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(min = 0.0, step = 1.0),
                visibility = versionRangeCondition("MIN_BOUND", "MIN_AND_MAX_BOUND"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.version.min.minor"),
                meta = UiNodeMeta(title = text("Minimum minor")),
                target = JsonPointer("/flags/*/rules/*/versionRange/min/minor"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(min = 0.0, step = 1.0),
                visibility = versionRangeCondition("MIN_BOUND", "MIN_AND_MAX_BOUND"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.version.min.patch"),
                meta = UiNodeMeta(title = text("Minimum patch")),
                target = JsonPointer("/flags/*/rules/*/versionRange/min/patch"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(min = 0.0, step = 1.0),
                visibility = versionRangeCondition("MIN_BOUND", "MIN_AND_MAX_BOUND"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.version.max.major"),
                meta = UiNodeMeta(title = text("Maximum major")),
                target = JsonPointer("/flags/*/rules/*/versionRange/max/major"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(min = 0.0, step = 1.0),
                visibility = versionRangeCondition("MAX_BOUND", "MIN_AND_MAX_BOUND"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.version.max.minor"),
                meta = UiNodeMeta(title = text("Maximum minor")),
                target = JsonPointer("/flags/*/rules/*/versionRange/max/minor"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(min = 0.0, step = 1.0),
                visibility = versionRangeCondition("MAX_BOUND", "MIN_AND_MAX_BOUND"),
            ),
            UiField(
                id = UiNodeId("konditional.rule.version.max.patch"),
                meta = UiNodeMeta(title = text("Maximum patch")),
                target = JsonPointer("/flags/*/rules/*/versionRange/max/patch"),
                valueKind = UiValueKind.INT,
                control = UiControlType.NUMBER,
                inputHints = UiInputHints(min = 0.0, step = 1.0),
                visibility = versionRangeCondition("MAX_BOUND", "MIN_AND_MAX_BOUND"),
            ),
        ),
    )

private fun flagValueTypeOptions(): List<UiOption> =
    listOf(
        option("BOOLEAN", "Boolean"),
        option("STRING", "String"),
        option("INT", "Integer"),
        option("DOUBLE", "Decimal"),
        option("ENUM", "Enum"),
        option("DATA_CLASS", "Data class"),
    )

private fun versionRangeOptions(): List<UiOption> =
    listOf(
        option("UNBOUNDED", "All versions"),
        option("MIN_BOUND", "Minimum version"),
        option("MAX_BOUND", "Maximum version"),
        option("MIN_AND_MAX_BOUND", "Version range"),
    )

private fun localeOptions(): List<UiOption> =
    listOf(
        option("AUSTRALIA", "Australia"),
        option("AUSTRIA", "Austria"),
        option("BELGIUM_DUTCH", "Belgium (Dutch)"),
        option("BELGIUM_FRENCH", "Belgium (French)"),
        option("CANADA", "Canada"),
        option("CANADA_FRENCH", "Canada (French)"),
        option("FINLAND", "Finland"),
        option("FRANCE", "France"),
        option("GERMANY", "Germany"),
        option("HONG_KONG", "Hong Kong"),
        option("HONG_KONG_ENGLISH", "Hong Kong (English)"),
        option("INDIA", "India"),
        option("ITALY", "Italy"),
        option("JAPAN", "Japan"),
        option("MEXICO", "Mexico"),
        option("NETHERLANDS", "Netherlands"),
        option("NEW_ZEALAND", "New Zealand"),
        option("NORWAY", "Norway"),
        option("SINGAPORE", "Singapore"),
        option("SPAIN", "Spain"),
        option("SWEDEN", "Sweden"),
        option("TAIWAN", "Taiwan"),
        option("UNITED_KINGDOM", "United Kingdom"),
        option("UNITED_STATES", "United States"),
        option("ICC_EN_EU", "ICC (EU English)"),
        option("ICC_EN_EI", "ICC (IE English)"),
    )

private fun platformOptions(): List<UiOption> =
    listOf(
        option("IOS", "iOS"),
        option("ANDROID", "Android"),
        option("WEB", "Web"),
    )

private fun typeCondition(pointer: JsonPointer, value: String): UiConditionGroup =
    UiConditionGroup(
        allOf = listOf(
            UiCondition(
                pointer = pointer,
                operator = UiConditionOperator.EQUALS,
                value = UiEnum(value),
            ),
        ),
    )

private fun versionRangeCondition(vararg values: String): UiConditionGroup =
    UiConditionGroup(
        anyOf = values.map { value ->
            UiCondition(
                pointer = JsonPointer("/flags/*/rules/*/versionRange/type"),
                operator = UiConditionOperator.EQUALS,
                value = UiEnum(value),
            )
        },
    )

private fun option(value: String, label: String): UiOption =
    UiOption(
        value = value,
        label = text(label),
    )

private fun text(value: String): UiText = UiTextLiteral(value)
