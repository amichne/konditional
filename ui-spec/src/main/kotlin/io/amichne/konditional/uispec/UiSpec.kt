package io.amichne.konditional.uispec

data class UiSpec(
    val version: String,
    val root: UiNode,
    val metadata: UiSpecMetadata = UiSpecMetadata(),
)

data class UiSpecMetadata(
    val title: UiText? = null,
    val description: UiText? = null,
    val tags: List<String> = emptyList(),
)
