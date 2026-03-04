package io.amichne.konditional.migration

import io.amichne.konditional.core.Context
import io.amichne.konditional.core.Feature

/**
 * Orchestrates dual-read evaluation during migration from legacy flag system
 * to Konditional.
 *
 * Returns baseline (legacy) value by default, with configurable gradual
 * promotion to candidate (Konditional) value. Emits mismatch telemetry when
 * baseline != candidate to enable verification before promotion.
 *
 * # Usage Pattern
 *
 * ```kotlin
 * val adapter = FlagMigrationAdapter(
 *     legacyClient = myLegacyClient,
 *     telemetry = OpenTelemetryMismatchTelemetry(),
 *     promotionRegistry = InMemoryPromotionRegistry()
 * )
 *
 * // Phase 1: Dual-read, always return baseline
 * val result = adapter.evaluateBoolean(
 *     context = myContext,
 *     legacyKey = "enable-feature-x",
 *     kandidate = MyFlags.enableFeatureX
 * )
 *
 * // Phase 2: After verification, promote gradually
 * adapter.promotionRegistry.promote("enable-feature-x", percentage = 10)
 *
 * // Phase 3: After stability, full promotion
 * adapter.promotionRegistry.promote("enable-feature-x", percentage = 100)
 * ```
 *
 * @param legacyClient The existing feature flag client being replaced
 * @param telemetry Mismatch reporting sink
 * @param promotionRegistry Controls per-flag promotion percentage
 */
class FlagMigrationAdapter<LegacyClient>(
    private val legacyClient: LegacyClient,
    private val telemetry: MismatchTelemetry,
    private val promotionRegistry: PromotionRegistry,
    private val legacyEvaluator: LegacyEvaluator<LegacyClient>
) {
    
    /**
     * Evaluate a boolean feature with dual-read safety.
     *
     * @param context Typed Konditional context
     * @param legacyKey String key from legacy system
     * @param kandidate Typed Konditional feature
     * @return baseline value (unless promoted)
     */
    fun <C : Context> evaluateBoolean(
        context: C,
        legacyKey: String,
        kandidate: Feature<C, Boolean>
    ): Boolean {
        val baseline = legacyEvaluator.evaluateBoolean(
            client = legacyClient,
            key = legacyKey,
            default = kandidate.default
        )
        val candidate = kandidate.evaluate(context)
        
        return selectAndReport(
            legacyKey = legacyKey,
            baseline = baseline,
            candidate = candidate,
            context = context
        )
    }
    
    /**
     * Evaluate a string feature with dual-read safety.
     */
    fun <C : Context> evaluateString(
        context: C,
        legacyKey: String,
        kandidate: Feature<C, String>
    ): String {
        val baseline = legacyEvaluator.evaluateString(
            client = legacyClient,
            key = legacyKey,
            default = kandidate.default
        )
        val candidate = kandidate.evaluate(context)
        
        return selectAndReport(
            legacyKey = legacyKey,
            baseline = baseline,
            candidate = candidate,
            context = context
        )
    }
    
    /**
     * Evaluate an integer feature with dual-read safety.
     */
    fun <C : Context> evaluateInteger(
        context: C,
        legacyKey: String,
        kandidate: Feature<C, Int>
    ): Int {
        val baseline = legacyEvaluator.evaluateInteger(
            client = legacyClient,
            key = legacyKey,
            default = kandidate.default
        )
        val candidate = kandidate.evaluate(context)
        
        return selectAndReport(
            legacyKey = legacyKey,
            baseline = baseline,
            candidate = candidate,
            context = context
        )
    }
    
    /**
     * Evaluate a double feature with dual-read safety.
     */
    fun <C : Context> evaluateDouble(
        context: C,
        legacyKey: String,
        kandidate: Feature<C, Double>
    ): Double {
        val baseline = legacyEvaluator.evaluateDouble(
            client = legacyClient,
            key = legacyKey,
            default = kandidate.default
        )
        val candidate = kandidate.evaluate(context)
        
        return selectAndReport(
            legacyKey = legacyKey,
            baseline = baseline,
            candidate = candidate,
            context = context
        )
    }
    
    private fun <T, C : Context> selectAndReport(
        legacyKey: String,
        baseline: T,
        candidate: T,
        context: C
    ): T {
        val isPromoted = promotionRegistry.isPromoted(legacyKey, context)
        val selected = if (isPromoted) candidate else baseline
        
        if (baseline != candidate) {
            telemetry.recordMismatch(
                legacyKey = legacyKey,
                baseline = baseline,
                candidate = candidate,
                selected = selected,
                context = context
            )
        }
        
        return selected
    }
}

/**
 * Abstraction over legacy flag client evaluation.
 *
 * Implement this interface to adapt your specific legacy SDK.
 */
interface LegacyEvaluator<LegacyClient> {
    fun evaluateBoolean(client: LegacyClient, key: String, default: Boolean): Boolean
    fun evaluateString(client: LegacyClient, key: String, default: String): String
    fun evaluateInteger(client: LegacyClient, key: String, default: Int): Int
    fun evaluateDouble(client: LegacyClient, key: String, default: Double): Double
}

/**
 * Mismatch telemetry sink.
 *
 * Emit structured events when baseline != candidate to enable:
 * - Root cause analysis before promotion
 * - Verification of migration correctness
 * - Detection of legacy system inconsistencies
 */
interface MismatchTelemetry {
    fun recordMismatch(
        legacyKey: String,
        baseline: Any?,
        candidate: Any?,
        selected: Any?,
        context: Context
    )
}

/**
 * Controls gradual promotion from baseline to candidate.
 *
 * Supports:
 * - Percentage-based rollout per flag
 * - Instant rollback to baseline
 * - Context-aware promotion (for example segment-based)
 */
interface PromotionRegistry {
    /**
     * Promote a flag to candidate value for a percentage of evaluations.
     *
     * @param legacyKey Legacy flag identifier
     * @param percentage 0-100, where 0 = always baseline, 100 = always candidate
     */
    fun promote(legacyKey: String, percentage: Int)
    
    /**
     * Check if this context should receive candidate value.
     *
     * @return true if promoted, false if baseline
     */
    fun isPromoted(legacyKey: String, context: Context): Boolean
    
    /**
     * Instant rollback to baseline for a flag.
     */
    fun rollback(legacyKey: String)
}

/**
 * Example: In-memory promotion registry with deterministic hashing.
 */
class InMemoryPromotionRegistry : PromotionRegistry {
    private val promotions = mutableMapOf<String, Int>()
    
    override fun promote(legacyKey: String, percentage: Int) {
        require(percentage in 0..100) { "Percentage must be 0-100, got $percentage" }
        promotions[legacyKey] = percentage
    }
    
    override fun isPromoted(legacyKey: String, context: Context): Boolean {
        val percentage = promotions[legacyKey] ?: 0
        if (percentage == 0) return false
        if (percentage == 100) return true
        
        val stableId = context.stableId?.value ?: return false
        val hash = "$legacyKey:$stableId".hashCode()
        val bucket = (hash.toLong() and 0xFFFFFFFFL) % 100
        
        return bucket < percentage
    }
    
    override fun rollback(legacyKey: String) {
        promotions[legacyKey] = 0
    }
}

// Example implementations for common legacy systems

/**
 * Example: LaunchDarkly SDK adapter
 */
class LaunchDarklyEvaluator : LegacyEvaluator<com.launchdarkly.sdk.server.LDClient> {
    override fun evaluateBoolean(
        client: com.launchdarkly.sdk.server.LDClient,
        key: String,
        default: Boolean
    ): Boolean {
        // Note: Would need to convert Context to LDContext
        return client.boolVariation(key, com.launchdarkly.sdk.LDContext.create(""), default)
    }
    
    override fun evaluateString(
        client: com.launchdarkly.sdk.server.LDClient,
        key: String,
        default: String
    ): String {
        return client.stringVariation(key, com.launchdarkly.sdk.LDContext.create(""), default)
    }
    
    override fun evaluateInteger(
        client: com.launchdarkly.sdk.server.LDClient,
        key: String,
        default: Int
    ): Int {
        return client.intVariation(key, com.launchdarkly.sdk.LDContext.create(""), default)
    }
    
    override fun evaluateDouble(
        client: com.launchdarkly.sdk.server.LDClient,
        key: String,
        default: Double
    ): Double {
        return client.doubleVariation(key, com.launchdarkly.sdk.LDContext.create(""), default)
    }
}

/**
 * Example: OpenTelemetry-backed mismatch telemetry
 */
class OpenTelemetryMismatchTelemetry(
    private val tracer: io.opentelemetry.api.trace.Tracer
) : MismatchTelemetry {
    override fun recordMismatch(
        legacyKey: String,
        baseline: Any?,
        candidate: Any?,
        selected: Any?,
        context: Context
    ) {
        val span = tracer.spanBuilder("konditional.migration.mismatch")
            .setAllAttributes(
                io.opentelemetry.api.common.Attributes.builder()
                    .put("flag.legacy_key", legacyKey)
                    .put("flag.baseline_value", baseline.toString())
                    .put("flag.candidate_value", candidate.toString())
                    .put("flag.selected_value", selected.toString())
                    .put("context.stable_id", context.stableId?.value ?: "")
                    .build()
            )
            .startSpan()
        
        span.end()
    }
}
