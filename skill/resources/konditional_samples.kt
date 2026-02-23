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
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.runtime.load
import io.amichne.konditional.runtime.rollback
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec

/**
 * Basic typed-variant feature with deterministic targeting.
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
 * Axis-based targeting with namespace-scoped axis catalog.
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
        enable { extension { isEnterpriseCustomer } }
    }
}

fun evaluateVariant(context: Context): CheckoutVariant =
    CheckoutFlags.variant.evaluate(context)

fun evaluateSegmentFlag(stableId: String): Boolean {
    val context =
        object : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext {
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
    val decoded = ConfigurationSnapshotCodec.decode(json, CheckoutFlags.compiledSchema())
    return decoded.fold(
        onSuccess = { materialized ->
            CheckoutFlags.load(materialized)
            Result.success(Unit)
        },
        onFailure = { throwable ->
            val parseError = decoded.parseErrorOrNull()
            Result.failure(IllegalStateException(parseError?.message ?: throwable.message ?: "unknown snapshot failure"))
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
