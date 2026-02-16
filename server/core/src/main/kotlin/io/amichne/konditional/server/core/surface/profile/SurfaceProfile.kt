package io.amichne.konditional.server.core.surface.profile

internal enum class SurfaceProfile(
    val wireValue: String,
) {
    DEV(wireValue = "Dev"),
    QA(wireValue = "QA"),
    PROD(wireValue = "Prod"),
}
