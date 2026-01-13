@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.demo

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.uiktor.UiReactPaths
import io.amichne.konditional.uiktor.UiReactRouteConfig
import io.amichne.konditional.uiktor.UiSnapshotService
import io.amichne.konditional.uiktor.installReactUiRoutes
import io.ktor.server.routing.Route
import java.util.concurrent.atomic.AtomicReference

private class InMemoryUiSnapshotService(initial: Configuration) : UiSnapshotService {
    private val current = AtomicReference(initial)

    override fun load() = current.get()

    override fun save(configuration: Configuration) {
        current.set(configuration)
    }
}

fun Route.installDemoKonditionalReactUi(
    paths: UiReactPaths = UiReactPaths(),
): Unit {
    registerDemoFeatures()
    installReactUiRoutes(
        UiReactRouteConfig(
            service = InMemoryUiSnapshotService(sampleConfiguration()),
            paths = paths,
        ),
    )
}

private fun sampleConfiguration(): Configuration =
    when (val result = sampleSnapshot().toConfiguration()) {
        is ParseResult.Success -> result.value
        is ParseResult.Failure -> error(result.error.message)
    }
