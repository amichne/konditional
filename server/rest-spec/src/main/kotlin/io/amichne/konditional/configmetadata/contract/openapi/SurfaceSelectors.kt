package io.amichne.konditional.configmetadata.contract.openapi

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
