package io.amichne.konditional.uispec

data class UiConditionGroup(
    val allOf: List<UiCondition> = emptyList(),
    val anyOf: List<UiCondition> = emptyList(),
    val noneOf: List<UiCondition> = emptyList(),
)

data class UiCondition(
    val pointer: JsonPointer,
    val operator: UiConditionOperator,
    val value: UiValue? = null,
)

enum class UiConditionOperator {
    EXISTS,
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN,
    MATCHES,
}
