package io.amichne.konditional.server.core.surface.dto

import io.amichne.konditional.server.core.surface.profile.SurfaceProfile
import io.amichne.konditional.server.core.surface.selector.TargetSelector

internal data class SnapshotMutationRequest(
    val namespaceId: String,
    val requestedBy: String,
    val reason: String,
    val selector: TargetSelector = TargetSelector.All,
    val profile: SurfaceProfile? = null,
)
