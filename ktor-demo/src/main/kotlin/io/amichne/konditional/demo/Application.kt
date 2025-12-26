package io.amichne.konditional.demo

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.configstate.ConfigurationStateFactory
import io.amichne.konditional.configstate.ConfigurationStateSerializer
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.serialization.SnapshotSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

// Extension properties for display names
private val AppLocale.displayName: String
    get() =
        name
            .split('_')
            .joinToString(separator = " ") { it.lowercase().replaceFirstChar(Char::uppercase) }

private val Platform.displayName: String
    get() = when (this) {
        Platform.IOS -> "iOS"
        Platform.ANDROID -> "Android"
        Platform.WEB -> "Web"
    }

private val SubscriptionTier.displayName: String
    get() = when (this) {
        SubscriptionTier.FREE -> "Free"
        SubscriptionTier.STARTER -> "Starter"
        SubscriptionTier.PROFESSIONAL -> "Professional"
        SubscriptionTier.ENTERPRISE -> "Enterprise"
    }

// Predefined HexId-compliant user IDs for testing
private object SampleHexIds {
    val USER_1 = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
    val USER_2 = "f1e2d3c4b5a6978685746352413021ab"
    val USER_3 = "123456789abcdef0fedcba9876543210"
    val USER_4 = "deadbeefcafebabe1234567890abcdef"
    val USER_5 = "0f1e2d3c4b5a69788574635241302100"

    val all = listOf(USER_1, USER_2, USER_3, USER_4, USER_5)
    val displayNames = listOf(
        "User Alpha",
        "User Beta",
        "User Gamma",
        "User Delta",
        "User Epsilon"
    )
}

fun main() {
    // Force initialization of feature containers by accessing their properties
    // This triggers the property delegation which registers features with the namespace
    println("[main] Initializing DemoFeatures...")
    with(DemoFeatures) {
        listOf(
            DARK_MODE, BETA_FEATURES, ANALYTICS_ENABLED, WELCOME_MESSAGE,
            THEME_COLOR, MAX_ITEMS_PER_PAGE, CACHE_TTL_SECONDS,
            DISCOUNT_PERCENTAGE, API_RATE_LIMIT,
            SSO_ENABLED, ADVANCED_ANALYTICS, CUSTOM_BRANDING, DEDICATED_SUPPORT,
        )
    }

    // Verify features are registered
    val configuration = DemoFeatures.configuration
    val snapshot = SnapshotSerializer.serialize(configuration)
    println("[main] Konfig snapshot length: ${snapshot.length}")
    if (snapshot.length < 100) {
        println("[main] WARNING: Snapshot is suspiciously small!")
        println("[main] Snapshot: $snapshot")
    } else {
        println("[main] Successfully initialized ${DemoFeatures.allFeatures().size} features")
    }

    println("[main] Starting server on port 8080...")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        // Serve static resources (compiled Kotlin/JS)
        staticResources("/static", "static")

        get("/") {
            call.respondHtml {
                renderMainPage()
            }
        }

        get("/configstate/catalog") {
            call.respondHtml {
                renderConfigStateCatalogPage()
            }
        }

        get("/api/configstate") {
            val response = ConfigurationStateFactory.from(DemoFeatures.configuration)
            call.respondText(ConfigurationStateSerializer.toJson(response), ContentType.Application.Json)
        }

        post("/api/evaluate") {
            try {
                val params = call.receiveParameters()
                val contextType = params["contextType"] ?: "base"

                val result = when (contextType) {
                    "enterprise" -> evaluateEnterpriseContext(params)
                    else -> evaluateBaseContext(params)
                }

                call.respondText(result, ContentType.Application.Json)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }
}

private fun buildRulesInfo(): String {
    println("[buildRulesInfo] Starting to build rules info")
    val moshi = com.squareup.moshi.Moshi.Builder().build()
    val adapter = moshi.adapter(Map::class.java)

    // Parse the snapshot to extract rule details
    val snapshot = SnapshotSerializer.serialize(DemoFeatures.configuration)
    println("[buildRulesInfo] Snapshot length: ${snapshot.length}")

    val snapshotData = moshi.adapter(Map::class.java).fromJson(snapshot)
    println("[buildRulesInfo] Snapshot parsed, keys: ${snapshotData?.keys}")

    val flags = snapshotData?.get("flags")
    println("[buildRulesInfo] Flags data: $flags")
    println("[buildRulesInfo] Flags type: ${flags?.javaClass?.name}")

    val featuresData = (snapshotData?.get("flags") as? List<*>)?.map { flagData ->
        val flag = flagData as? Map<*, *>
        val key = flag?.get("key") as? String ?: "unknown"

        // Get rules array (not "values")
        val rules = flag?.get("rules") as? List<*> ?: emptyList<Any>()

        // Get defaultValue object and extract the actual value
        val defaultValueObj = flag?.get("defaultValue") as? Map<*, *>
        val defaultValue = defaultValueObj?.get("value")
        val typeStr = defaultValueObj?.get("type") as? String ?: "Unknown"

        println("[buildRulesInfo] Processing flag: key=$key, rulesCount=${rules.size}, default=$defaultValue, type=$typeStr")

        mapOf(
            "key" to key,
            "type" to typeStr.lowercase().replaceFirstChar { it.uppercase() }, // "BOOLEAN" -> "Boolean"
            "default" to defaultValue,
            "rulesCount" to rules.size,
            "hasRules" to rules.isNotEmpty(),
            "rules" to rules.map { moshi.adapter(List::class.java).toJsonValue(rules) }
        )
    } ?: emptyList()

    println("[buildRulesInfo] Features data count: ${featuresData.size}")

    val rulesMap = mapOf("features" to featuresData)
    val result = adapter.toJson(rulesMap)
    println("[buildRulesInfo] Final JSON: $result")
    return result
}

private fun evaluateBaseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "UNITED_STATES")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parse(params["version"] ?: "1.0.0").getOrThrow()
    val stableIdHex = params["stableId"] ?: "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
    val stableId = StableId.fromHex(stableIdHex)

    val context = DemoContext(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = stableId
    )

    return buildEvaluationJson(context)
}

private fun evaluateEnterpriseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "UNITED_STATES")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parse(params["version"] ?: "1.0.0").getOrThrow()
    val stableIdHex = params["stableId"] ?: "f1e2d3c4b5a6978685746352413021ab"
    val stableId = StableId.fromHex(stableIdHex)
    val subscriptionTier = SubscriptionTier.fromString(params["subscriptionTier"] ?: "FREE")
    val organizationId = params["organizationId"] ?: "org-001"
    val employeeCount = params["employeeCount"]?.toIntOrNull() ?: 10

    val context = EnterpriseContext(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = stableId,
        subscriptionTier = subscriptionTier,
        organizationId = organizationId,
        employeeCount = employeeCount
    )

    return buildEnterpriseEvaluationJson(context)
}

private fun buildEvaluationJson(context: DemoContext): String {
    val results = mutableMapOf<String, Any>()

    results["darkMode"] = DemoFeatures.DARK_MODE.evaluate(context)
    results["betaFeatures"] = DemoFeatures.BETA_FEATURES.evaluate(context)
    results["analyticsEnabled"] = DemoFeatures.ANALYTICS_ENABLED.evaluate(context)
    results["welcomeMessage"] = DemoFeatures.WELCOME_MESSAGE.evaluate(context)
    results["themeColor"] = DemoFeatures.THEME_COLOR.evaluate(context)
    results["maxItemsPerPage"] = DemoFeatures.MAX_ITEMS_PER_PAGE.evaluate(context)
    results["cacheTtlSeconds"] = DemoFeatures.CACHE_TTL_SECONDS.evaluate(context)
    results["discountPercentage"] = DemoFeatures.DISCOUNT_PERCENTAGE.evaluate(context)
    results["apiRateLimit"] = DemoFeatures.API_RATE_LIMIT.evaluate(context)

    return com.squareup.moshi.Moshi.Builder().build()
        .adapter(Map::class.java)
        .toJson(results)
}

private fun buildEnterpriseEvaluationJson(context: EnterpriseContext): String {
    val results = mutableMapOf<String, Any>()

    // Evaluate base features with enterprise context
    results["darkMode"] = DemoFeatures.DARK_MODE.evaluate(context)
    results["betaFeatures"] = DemoFeatures.BETA_FEATURES.evaluate(context)
    results["analyticsEnabled"] = DemoFeatures.ANALYTICS_ENABLED.evaluate(context)
    results["welcomeMessage"] = DemoFeatures.WELCOME_MESSAGE.evaluate(context)
    results["themeColor"] = DemoFeatures.THEME_COLOR.evaluate(context)
    results["maxItemsPerPage"] = DemoFeatures.MAX_ITEMS_PER_PAGE.evaluate(context)
    results["cacheTtlSeconds"] = DemoFeatures.CACHE_TTL_SECONDS.evaluate(context)
    results["discountPercentage"] = DemoFeatures.DISCOUNT_PERCENTAGE.evaluate(context)
    results["apiRateLimit"] = DemoFeatures.API_RATE_LIMIT.evaluate(context)

    // Evaluate enterprise features
    results["ssoEnabled"] = DemoFeatures.SSO_ENABLED.evaluate(context)
    results["advancedAnalytics"] = DemoFeatures.ADVANCED_ANALYTICS.evaluate(context)
    results["customBranding"] = DemoFeatures.CUSTOM_BRANDING.evaluate(context)
    results["dedicatedSupport"] = DemoFeatures.DEDICATED_SUPPORT.evaluate(context)

    return com.squareup.moshi.Moshi.Builder().build()
        .adapter(Map::class.java)
        .toJson(results)
}

private fun HTML.renderMainPage() {
    head {
        title { +"Konditional Feature Evaluation Demo" }
        style {
            unsafe {
                raw(
                    """
                    body {
                        margin: 0;
                        padding: 0;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background-color: #f5f5f5;
                    }
                    """.trimIndent()
                )
            }
        }
    }
    body {
        // React root element
        div {
            id = "featureEvaluationRoot"
        }

        // Embed snapshot and rules data directly in HTML (for backward compatibility)
        script {
            unsafe {
                raw(
                    """
                    window.KONDITIONAL_RULES = ${buildRulesInfo()};
                    window.KONDITIONAL_SNAPSHOT = ${SnapshotSerializer.serialize(DemoFeatures.configuration)};
                """.trimIndent()
                )
            }
        }

        // Load compiled Kotlin/JS client code
        script {
            src = "/static/demo-client.js"
        }
    }
}

private fun HTML.renderConfigStateCatalogPage() {
    head {
        title { +"Konditional ConfigState UI Catalog" }
        style {
            unsafe {
                raw(
                    """
                    body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                    """.trimIndent(),
                )
            }
        }
    }
    body {
        div {
            id = "configstateCatalogRoot"
        }
        script {
            src = "/static/demo-client.js"
        }
    }
}
