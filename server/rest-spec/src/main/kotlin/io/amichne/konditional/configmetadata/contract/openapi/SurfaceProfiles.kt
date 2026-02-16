package io.amichne.konditional.configmetadata.contract.openapi

internal enum class SurfaceProfile(
    val wireValue: String,
) {
    DEV(wireValue = "Dev"),
    QA(wireValue = "QA"),
    PROD(wireValue = "Prod"),
}

internal enum class SurfaceCapability {
    READ_SNAPSHOT,
    READ_NAMESPACE_SNAPSHOT,
    READ_FEATURE,
    READ_RULE,
    MUTATE_SNAPSHOT_PATCH,
    MUTATE_SNAPSHOT_LEGACY_POST,
    PATCH_NAMESPACE,
    CREATE_FEATURE,
    PATCH_FEATURE,
    PATCH_RULE,
}

internal data class CapabilityProfile(
    val profile: SurfaceProfile,
    val capabilities: Set<SurfaceCapability>,
)

internal object SurfaceCapabilityProfiles {
    private val devProfile =
        CapabilityProfile(
            profile = SurfaceProfile.DEV,
            capabilities = SurfaceCapability.entries.toSet(),
        )

    private val qaProfile =
        CapabilityProfile(
            profile = SurfaceProfile.QA,
            capabilities = SurfaceCapability.entries.toSet(),
        )

    private val prodProfile =
        CapabilityProfile(
            profile = SurfaceProfile.PROD,
            capabilities = SurfaceCapability.entries.toSet() - SurfaceCapability.MUTATE_SNAPSHOT_LEGACY_POST,
        )

    private val profilesByName: Map<SurfaceProfile, CapabilityProfile> =
        linkedMapOf(
            devProfile.profile to devProfile,
            qaProfile.profile to qaProfile,
            prodProfile.profile to prodProfile,
        )

    fun supports(
        profile: SurfaceProfile,
        capability: SurfaceCapability,
    ): Boolean =
        profilesByName
            .getValue(profile)
            .capabilities
            .contains(capability)

    fun byProfile(profile: SurfaceProfile): CapabilityProfile = profilesByName.getValue(profile)

    fun profilesFor(capability: SurfaceCapability): Set<SurfaceProfile> =
        profilesByName.entries
            .filter { (_, profileDefinition) -> profileDefinition.capabilities.contains(capability) }
            .mapTo(linkedSetOf()) { (profile, _) -> profile }
}
