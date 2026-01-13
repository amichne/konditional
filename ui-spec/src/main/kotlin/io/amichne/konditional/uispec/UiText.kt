package io.amichne.konditional.uispec

sealed interface UiText {
    val kind: UiTextKind
}

enum class UiTextKind {
    LITERAL,
    KEY,
}

data class UiTextLiteral(val value: String) : UiText {
    override val kind: UiTextKind = UiTextKind.LITERAL
}

data class UiTextKey(
    val key: String,
    val fallback: String? = null,
) : UiText {
    override val kind: UiTextKind = UiTextKind.KEY
}
