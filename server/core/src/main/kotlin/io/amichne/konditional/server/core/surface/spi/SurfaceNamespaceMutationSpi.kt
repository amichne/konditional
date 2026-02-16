package io.amichne.konditional.server.core.surface.spi

import io.amichne.konditional.server.core.surface.dto.FeatureCreateRequest
import io.amichne.konditional.server.core.surface.dto.FeaturePatchRequest
import io.amichne.konditional.server.core.surface.dto.MutationEnvelope
import io.amichne.konditional.server.core.surface.dto.NamespacePatchRequest
import io.amichne.konditional.server.core.surface.dto.RulePatchRequest
import io.amichne.konditional.server.core.surface.dto.SnapshotMutationRequest

internal interface SurfaceNamespaceMutationSpi {
    fun patchSnapshot(request: SnapshotMutationRequest): MutationEnvelope

    fun mutateSnapshotLegacy(request: SnapshotMutationRequest): MutationEnvelope

    fun patchNamespace(
        namespaceId: String,
        request: NamespacePatchRequest,
    ): MutationEnvelope

    fun createFeature(
        namespaceId: String,
        request: FeatureCreateRequest,
    ): MutationEnvelope

    fun patchFeature(
        namespaceId: String,
        featureKey: String,
        request: FeaturePatchRequest,
    ): MutationEnvelope

    fun patchRule(
        namespaceId: String,
        featureKey: String,
        ruleId: String,
        request: RulePatchRequest,
    ): MutationEnvelope
}
