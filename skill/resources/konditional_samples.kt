@file:Suppress("unused")

package skill.resources

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateWithShadow
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.openfeature.KonditionalOpenFeatureProvider
import io.amichne.konditional.openfeature.TargetingKeyContext
import io.amichne.konditional.openfeature.TargetingKeyContextMapper
import io.amichne.konditional.runtime.load
import io.amichne.konditional.runtime.rollback
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import dev.openfeature.sdk.ImmutableContext

/**
 * Enterprise baseline namespace with typed feature definitions.
 */
enum class CheckoutVariant {
    CLASSIC,
    FAST_PATH,
    NEW_UI,
}

object CheckoutFlags : Namespace("checkout") {
    val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
        rule(CheckoutVariant.FAST_PATH) { rampUp { 10.0 } }
        rule(CheckoutVariant.NEW_UI) { rampUp { 1.0 } }
    }

    val expressCheckout by boolean<Context>(default = false) {
        enable {
            platforms(Platform.IOS)
            versions { min(3, 0, 0) }
        }
    }
}

/**
 * Axis-based targeting where type inference remains namespace-scoped.
 */
enum class Segment(override val id: String) : AxisValue<Segment> {
    CONSUMER("consumer"),
    ENTERPRISE("enterprise"),
}

object SegmentFlags : Namespace("segment") {
    val segmentAxis = axis<Segment>("segment")

    val premiumUi by boolean<Context>(default = false) {
        enable { axis(Segment.ENTERPRISE) }
    }
}

/**
 * Context extension pattern for business-specific targeting.
 */
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val isEnterpriseCustomer: Boolean,
) : Context,
    Context.LocaleContext,
    Context.PlatformContext,
    Context.VersionContext,
    Context.StableIdContext

object EnterpriseFlags : Namespace("enterprise") {
    val advancedReporting by boolean<EnterpriseContext>(default = false) {
        /** Leaking the extended context properties into the core Context interface would be undesirable;
         *  instead, we can target them within the extension block. */
        enable { extension { isEnterpriseCustomer } }
    }

    val whenContext by boolean<EnterpriseContext>(default = false) {
        enable {
            /** Targeting that depends on the extended context properties,
             *  without leaking them into the core Context interface */
            whenContext<EnterpriseContext> {
                subscriptionTier == SubscriptionTier.ENTERPRISE
            }
        }
    }
}

/**
 * Legacy-to-Konditional inventory row used during discovery.
 */
data class LegacyFlagUsage(
    val key: String,
    val owningDomain: String,
    val valueType: String,
)

data class NamespaceAdoptionPlan(
    val namespaceId: String,
    val legacyKeys: List<String>,
)

/**
 * Deterministic mapping from discovered legacy keys to namespace plans.
 */
fun buildNamespacePlans(usages: List<LegacyFlagUsage>): List<NamespaceAdoptionPlan> =
    usages
        .sortedWith(compareBy(LegacyFlagUsage::owningDomain, LegacyFlagUsage::key))
        .groupBy { it.owningDomain.trim().replace(' ', '-').lowercase() }
        .map { (namespaceId, rows) ->
            NamespaceAdoptionPlan(
                namespaceId = namespaceId,
                legacyKeys = rows.map(LegacyFlagUsage::key),
            )
        }
        .sortedBy(NamespaceAdoptionPlan::namespaceId)

/**
 * Legacy SDK abstraction for dual-read migration.
 */
data class LegacyEvaluationContext(
    val stableId: String,
    val attributes: Map<String, String>,
)

fun interface LegacyBooleanFlagClient {
    fun getBoolean(
        key: String,
        defaultValue: Boolean,
        context: LegacyEvaluationContext,
    ): Boolean
}

data class DualReadDecision(
    val baseline: Boolean,
    val candidate: Boolean,
    val mismatch: Boolean,
)

/**
 * Migration adapter that keeps legacy behavior as baseline while comparing Konditional.
 */
class DualReadBooleanAdapter(
    private val legacyClient: LegacyBooleanFlagClient,
    private val onMismatch: (legacyKey: String, baseline: Boolean, candidate: Boolean, stableId: StableId) -> Unit =
        { _, _, _, _ -> },
) {
    fun evaluate(
        legacyKey: String,
        candidateFeature: io.amichne.konditional.core.features.Feature<Boolean, EnterpriseContext, *>,
        context: EnterpriseContext,
    ): DualReadDecision {
        val baseline =
            legacyClient.getBoolean(
                key = legacyKey,
                defaultValue = false,
                context =
                    LegacyEvaluationContext(
                        stableId = context.stableId.id,
                        attributes =
                            mapOf(
                                "locale" to context.locale.id,
                                "platform" to context.platform.id,
                                "version" to context.appVersion.toString(),
                            ),
                    ),
            )
        val candidate = candidateFeature.evaluate(context)
        if (baseline != candidate) {
            onMismatch(legacyKey, baseline, candidate, context.stableId)
        }
        return DualReadDecision(baseline = baseline, candidate = candidate, mismatch = baseline != candidate)
    }
}

fun evaluateVariant(context: Context): CheckoutVariant =
    CheckoutFlags.variant.evaluate(context)

fun evaluateSegmentFlag(stableId: String): Boolean {
    val context =
        object : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext,
                 Context.StableIdContext {
            override val locale: AppLocale = AppLocale.UNITED_STATES
            override val platform: Platform = Platform.ANDROID
            override val appVersion: Version = Version.of(3, 1, 0)
            override val stableId: StableId = StableId.of(stableId)
            override val axisValues = axisValues { set(SegmentFlags.segmentAxis, Segment.ENTERPRISE) }
        }
    return SegmentFlags.premiumUi.evaluate(context)
}

/**
 * Parse-don't-validate boundary for remote config ingestion.
 */
fun loadSnapshot(json: String): Result<Unit> {
    val loader = NamespaceSnapshotLoader.forNamespace(CheckoutFlags)
    val loaded = loader.load(json, options = SnapshotLoadOptions.fillMissingDeclaredFlags())
    return loaded.fold(
        onSuccess = {
            Result.success(Unit)
        },
        onFailure = { throwable ->
            val parseError = loaded.parseErrorOrNull()
            Result.failure(
                IllegalStateException(
                    parseError.withFallbackMessage(throwable.message ?: "unknown snapshot failure"),
                ),
            )
        },
    )
}

fun rollbackLastSnapshot(): Boolean = CheckoutFlags.rollback(steps = 1)

/**
 * Shadow evaluation sample: baseline result is returned, candidate only informs mismatch reporting.
 */
fun evaluateWithCandidate(context: Context, candidateJson: String): Boolean {
    val candidate = ConfigurationSnapshotCodec.decode(candidateJson, CheckoutFlags.compiledSchema()).getOrThrow()
    val candidateRegistry =
        InMemoryNamespaceRegistry(namespaceId = CheckoutFlags.namespaceId).apply {
            load(candidate.configuration)
        }

    return CheckoutFlags.expressCheckout.evaluateWithShadow(
        context = context,
        candidateRegistry = candidateRegistry,
        onMismatch = { mismatch ->
            println("shadow mismatch key=${mismatch.featureKey} kinds=${mismatch.kinds}")
        },
    )
}

/**
 * OpenFeature bridge using typed context mapper.
 */
fun buildOpenFeatureProvider(): KonditionalOpenFeatureProvider<TargetingKeyContext> =
    KonditionalOpenFeatureProvider(
        namespaceRegistry = CheckoutFlags,
        contextMapper = TargetingKeyContextMapper(),
    )

fun evaluateThroughOpenFeature(targetingKey: String): Boolean {
    val provider = buildOpenFeatureProvider()
    val evaluation = provider.getBooleanEvaluation("expressCheckout", false, ImmutableContext(targetingKey))
    return evaluation.value
}

private fun ParseError?.withFallbackMessage(fallback: String): String =
    this?.message ?: fallback
