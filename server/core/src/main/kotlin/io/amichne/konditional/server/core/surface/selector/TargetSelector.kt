package io.amichne.konditional.server.core.surface.selector

internal sealed interface TargetSelector {
    val kind: String

    data object All : TargetSelector {
        override val kind: String = "ALL"
    }

    data class Subset(
        val selectors: List<ScopedTargetSelector>,
    ) : TargetSelector {
        override val kind: String = "SUBSET"
    }
}
