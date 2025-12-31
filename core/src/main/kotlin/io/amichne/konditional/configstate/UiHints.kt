package io.amichne.konditional.configstate

data class UiHints(
    val control: UiControlType,
    val label: String? = null,
    val helpText: String? = null,
    val placeholder: String? = null,
    val advanced: Boolean = false,
    val order: Int? = null,
)
