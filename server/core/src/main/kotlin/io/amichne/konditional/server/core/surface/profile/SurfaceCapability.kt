package io.amichne.konditional.server.core.surface.profile

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
