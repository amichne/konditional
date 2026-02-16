package io.amichne.konditional.server.core.surface.spi

import io.amichne.konditional.server.core.surface.dto.FeatureEnvelope
import io.amichne.konditional.server.core.surface.dto.RuleEnvelope
import io.amichne.konditional.server.core.surface.dto.SnapshotEnvelope
import io.amichne.konditional.server.core.surface.selector.TargetSelector

internal interface SurfaceNamespaceReadSpi {
    fun readSnapshot(selector: TargetSelector): SnapshotEnvelope

    fun readNamespaceSnapshot(namespaceId: String): SnapshotEnvelope

    fun readFeature(
        namespaceId: String,
        featureKey: String,
    ): FeatureEnvelope

    fun readRule(
        namespaceId: String,
        featureKey: String,
        ruleId: String,
    ): RuleEnvelope
}
