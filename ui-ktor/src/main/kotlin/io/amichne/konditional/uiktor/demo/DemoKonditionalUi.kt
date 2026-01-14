@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.demo

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.uiktor.KonditionalSnapshotValueProvider
import io.amichne.konditional.uiktor.UiPatchResult
import io.amichne.konditional.uiktor.UiRenderSettings
import io.amichne.konditional.uiktor.UiRenderer
import io.amichne.konditional.uiktor.UiRouteConfig
import io.amichne.konditional.uiktor.UiRoutePaths
import io.amichne.konditional.uiktor.UiSpecService
import io.amichne.konditional.uiktor.defaultRenderer
import io.amichne.konditional.uiktor.installFlagListRoute
import io.amichne.konditional.uiktor.installUiRoutes
import io.amichne.konditional.uispec.UiPatchOperation
import io.amichne.konditional.uispec.UiSpec
import io.amichne.konditional.uispec.konditional.konditionalUiSpec
import io.amichne.konditional.values.FeatureId
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.Route

class DemoKonditionalUiService : UiSpecService<KonditionalSnapshotValueProvider> {
    private val snapshot = sampleSnapshot()

    override fun loadSpec(): UiSpec = konditionalUiSpec()

    override fun loadState(): KonditionalSnapshotValueProvider = KonditionalSnapshotValueProvider(snapshot)

    override fun applyPatch(
        state: KonditionalSnapshotValueProvider,
        patch: List<UiPatchOperation>,
    ): UiPatchResult<KonditionalSnapshotValueProvider> = UiPatchResult(state)

    fun getSnapshot(): SerializableSnapshot = snapshot
}

fun demoRenderer(
    paths: UiRoutePaths = UiRoutePaths(),
): UiRenderer<KonditionalSnapshotValueProvider> =
    defaultRenderer(UiRenderSettings(paths = paths))

fun demoUiRouteConfig(
    service: DemoKonditionalUiService,
    paths: UiRoutePaths = UiRoutePaths(),
): UiRouteConfig<KonditionalSnapshotValueProvider> =
    UiRouteConfig(
        service = service,
        renderer = demoRenderer(paths),
        paths = paths,
    )

fun Route.installDemoKonditionalUi(
    paths: UiRoutePaths = UiRoutePaths(),
): Unit =
    run {
        val service = DemoKonditionalUiService()
        registerDemoFeatures()

        val nodePrefix = paths.node.substringBefore("{id}")
        intercept(ApplicationCallPipeline.Monitoring) {
            val requestPath = call.request.path()
            val matches =
                requestPath == paths.page ||
                    requestPath == paths.patch ||
                    (nodePrefix.isNotBlank() && requestPath.startsWith(nodePrefix))
            if (matches) {
                application.environment.log.info(
                    "Konditional UI request: method={}, path={}",
                    call.request.httpMethod.value,
                    requestPath,
                )
            }
        }
        staticResources("/static", "static")
        installFlagListRoute(service.getSnapshot(), paths)
        installUiRoutes(demoUiRouteConfig(service, paths))
    }

internal fun sampleSnapshot(): SerializableSnapshot =
    SerializableSnapshot(
        flags = listOf(
            SerializableFlag(
                key = FeatureId.create("ui", "dark_mode_enabled"),
                defaultValue = FlagValue.BooleanValue(false),
                isActive = true,
                rampUpAllowlist = emptySet(),
                rules = listOf(
                    SerializableRule(
                        value = FlagValue.BooleanValue(true),
                        rampUp = 100.0,
                        note = "Enable for all users",
                        locales = setOf("UNITED_STATES", "CANADA"),
                        platforms = setOf("IOS", "ANDROID"),
                        versionRange = Unbounded(),
                        axes = mapOf("tier" to setOf("beta")),
                    ),
                ),
            ),
            SerializableFlag(
                key = FeatureId.create("payments", "provider"),
                defaultValue = FlagValue.StringValue("STRIPE"),
                isActive = true,
                rules = emptyList(),
            ),
        ),
    )
