package io.amichne.konditional.configmetadata.contract.openapi

internal interface SurfaceCodecSpi<in RequestPayload : Any, out ResponsePayload : Any> {
    fun decodeRequest(payload: RequestPayload): CodecOutcome

    fun encodeResponse(outcome: CodecOutcome): ResponsePayload
}

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
