package io.amichne.konditional.server.core.surface.profile

internal data class CapabilityProfile(
    val profile: SurfaceProfile,
    val capabilities: Set<SurfaceCapability>,
)
