package io.amichne.konditional.uispec

sealed interface UiNode {
    val id: UiNodeId
    val meta: UiNodeMeta
    val visibility: UiConditionGroup?
    val enabled: UiConditionGroup?
    val actions: List<UiAction>
}

data class UiNodeMeta(
    val title: UiText? = null,
    val description: UiText? = null,
    val helpText: UiText? = null,
    val icon: String? = null,
    val tags: List<String> = emptyList(),
)

data class UiPage(
    override val id: UiNodeId,
    override val meta: UiNodeMeta = UiNodeMeta(),
    val layout: UiLayout = UiLayout(),
    val children: List<UiNode> = emptyList(),
    override val actions: List<UiAction> = emptyList(),
    override val visibility: UiConditionGroup? = null,
    override val enabled: UiConditionGroup? = null,
) : UiNode

data class UiSection(
    override val id: UiNodeId,
    override val meta: UiNodeMeta = UiNodeMeta(),
    val layout: UiLayout = UiLayout(),
    val children: List<UiNode> = emptyList(),
    val collapsible: Boolean = false,
    val collapsedByDefault: Boolean = false,
    override val actions: List<UiAction> = emptyList(),
    override val visibility: UiConditionGroup? = null,
    override val enabled: UiConditionGroup? = null,
) : UiNode

data class UiGroup(
    override val id: UiNodeId,
    override val meta: UiNodeMeta = UiNodeMeta(),
    val layout: UiLayout = UiLayout(),
    val children: List<UiNode> = emptyList(),
    override val actions: List<UiAction> = emptyList(),
    override val visibility: UiConditionGroup? = null,
    override val enabled: UiConditionGroup? = null,
) : UiNode

data class UiTabs(
    override val id: UiNodeId,
    override val meta: UiNodeMeta = UiNodeMeta(),
    val tabs: List<UiTab> = emptyList(),
    override val actions: List<UiAction> = emptyList(),
    override val visibility: UiConditionGroup? = null,
    override val enabled: UiConditionGroup? = null,
) : UiNode

data class UiTab(
    val id: UiNodeId,
    val meta: UiNodeMeta = UiNodeMeta(),
    val children: List<UiNode> = emptyList(),
    val visibility: UiConditionGroup? = null,
    val enabled: UiConditionGroup? = null,
)

data class UiCollection(
    override val id: UiNodeId,
    override val meta: UiNodeMeta = UiNodeMeta(),
    val itemsPointer: JsonPointer,
    val itemKeyPointer: JsonPointer? = null,
    val itemTemplate: UiNode,
    val emptyState: UiEmptyState? = null,
    val addAction: UiAction? = null,
    val removeAction: UiAction? = null,
    val moveAction: UiAction? = null,
    override val actions: List<UiAction> = emptyList(),
    override val visibility: UiConditionGroup? = null,
    override val enabled: UiConditionGroup? = null,
) : UiNode

data class UiEmptyState(
    val title: UiText,
    val description: UiText? = null,
    val actions: List<UiAction> = emptyList(),
)

data class UiField(
    override val id: UiNodeId,
    override val meta: UiNodeMeta = UiNodeMeta(),
    val target: JsonPointer,
    val valueKind: UiValueKind,
    val control: UiControlType,
    val options: List<UiOption> = emptyList(),
    val inputHints: UiInputHints = UiInputHints(),
    val readOnly: Boolean = false,
    val required: Boolean = false,
    val placeholder: UiText? = null,
    override val actions: List<UiAction> = emptyList(),
    override val visibility: UiConditionGroup? = null,
    override val enabled: UiConditionGroup? = null,
) : UiNode

data class UiOption(
    val value: String,
    val label: UiText? = null,
    val description: UiText? = null,
)

data class UiInputHints(
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val pattern: String? = null,
    val rows: Int? = null,
    val unit: UiText? = null,
    val suggestions: List<String> = emptyList(),
)

enum class UiControlType {
    TOGGLE,
    TEXT,
    TEXTAREA,
    NUMBER,
    PERCENT,
    SELECT,
    MULTISELECT,
    KEY_VALUE,
    JSON,
    SEMVER,
    SEMVER_RANGE,
}

data class UiLayout(
    val direction: UiLayoutDirection = UiLayoutDirection.VERTICAL,
    val columns: Int? = null,
    val spacing: UiSpacing = UiSpacing.MD,
    val density: UiDensity = UiDensity.COMFORTABLE,
)

enum class UiLayoutDirection {
    VERTICAL,
    HORIZONTAL,
}

enum class UiSpacing {
    NONE,
    XS,
    SM,
    MD,
    LG,
    XL,
}

enum class UiDensity {
    COMPACT,
    COMFORTABLE,
    SPACIOUS,
}
