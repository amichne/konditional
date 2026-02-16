package io.amichne.konditional.server.core.surface.selector

internal sealed interface ScopedTargetSelector {
    val kind: String

    data class Namespace(
        val namespaceId: String,
    ) : ScopedTargetSelector {
        override val kind: String = "NAMESPACE"
    }

    data class Feature(
        val namespaceId: String,
        val featureKey: String,
    ) : ScopedTargetSelector {
        override val kind: String = "FEATURE"
    }

    data class Rule(
        val namespaceId: String,
        val featureKey: String,
        val ruleId: String,
    ) : ScopedTargetSelector {
        override val kind: String = "RULE"
    }
}
