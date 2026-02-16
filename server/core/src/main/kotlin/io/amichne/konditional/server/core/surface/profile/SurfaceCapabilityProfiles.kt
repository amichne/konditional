package io.amichne.konditional.server.core.surface.profile

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
