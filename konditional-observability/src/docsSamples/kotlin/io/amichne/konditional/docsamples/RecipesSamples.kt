@file:Suppress("unused")

package io.amichne.konditional.docsamples

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateWithShadow
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.ops.KonditionalLogger
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.ops.MetricsCollector
import io.amichne.konditional.core.ops.RegistryHooks
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.runtime.load
import io.amichne.konditional.runtime.rollback
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema

private object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
}

private fun renderClassic() = Unit

private fun renderFastPath() = Unit

private fun renderNewUi() = Unit

private fun applyDarkMode(enabled: Boolean) = enabled

private fun fetchRemoteConfig(): String = "{}"

private fun fetchCandidateConfig(): String = "{}"

private object AppLogger {
    fun warn(message: String, throwable: Throwable?) = Unit
}

private object AppMetrics {
    fun increment(name: String, tags: Map<String, String>) = Unit
}

private object RecipeLogger {
    fun error(message: () -> String) = Unit

    fun warn(message: () -> String) = Unit
}

// region recipe-1-typed-variants
enum class CheckoutVariant { CLASSIC, FAST_PATH, NEW_UI }

object CheckoutFlags : Namespace("checkout") {
    val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
        rule(CheckoutVariant.FAST_PATH) { rampUp { 10.0 } }
        rule(CheckoutVariant.NEW_UI) { rampUp { 1.0 } }
    }
}

fun renderCheckout(context: Context) {
    when (CheckoutFlags.variant.evaluate(context)) {
        CheckoutVariant.CLASSIC -> renderClassic()
        CheckoutVariant.FAST_PATH -> renderFastPath()
        CheckoutVariant.NEW_UI -> renderNewUi()
    }
}
// endregion recipe-1-typed-variants

// region recipe-2-rampup
object RampUpFlags : Namespace("ramp-up") {
    val newCheckout by boolean<Context>(default = false) {
        salt("v1")
        enable { rampUp { 10.0 } }
    }
}

fun isCheckoutEnabled(context: Context): Boolean =
    RampUpFlags.newCheckout.evaluate(context)
// endregion recipe-2-rampup

// region recipe-2-reset
object RampUpResetFlags : Namespace("ramp-up-reset") {
    val newCheckout by boolean<Context>(default = false) {
        salt("v2")
        enable { rampUp { 10.0 } }
    }
}
// endregion recipe-2-reset

// region recipe-3-axes
enum class Segment(override val id: String) : AxisValue<Segment> {
    CONSUMER("consumer"),
    SMB("smb"),
    ENTERPRISE("enterprise"),
}

object Axes {
    object SegmentAxis : Axis<Segment>("segment", Segment::class)
}

object SegmentFlags : Namespace("segment") {
    @Suppress("UnusedPrivateProperty")
    private val segmentAxis = Axes.SegmentAxis

    val premiumUi by boolean<Context>(default = false) {
        enable { axis(Segment.ENTERPRISE) }
    }
}

fun isPremiumUiEnabled(): Boolean {
    val segmentContext =
        object :
            Context,
            Context.LocaleContext,
            Context.PlatformContext,
            Context.VersionContext,
            Context.StableIdContext {
            override val locale = AppLocale.UNITED_STATES
            override val platform = Platform.IOS
            override val appVersion = Version.of(2, 1, 0)
            override val stableId = StableId.of("user-123")
            override val axisValues = axisValues { +Segment.ENTERPRISE }
        }

    return SegmentFlags.premiumUi.evaluate(segmentContext)
}
// endregion recipe-3-axes

// region recipe-4-extension
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int,
) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }

object PremiumFeatures : Namespace("premium") {
    val advancedAnalytics by boolean<EnterpriseContext>(default = false) {
        enable {
            extension { subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100 }
        }
    }
}
// endregion recipe-4-extension

// region recipe-5-structured
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
) : Konstrained<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
        ::enabled of { default = true }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy()) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) { platforms(Platform.ANDROID) }
    }
}
// endregion recipe-5-structured

// region recipe-6-load
fun loadRemoteConfig() {
    val json = fetchRemoteConfig()
    val features = AppFeatures

    when (val result = ConfigurationSnapshotCodec.decode(json)) {
        is ParseResult.Success -> features.load(result.value)
        is ParseResult.Failure -> RecipeLogger.error { "Config rejected: ${result.error.message}" }
    }
}
// endregion recipe-6-load

// region recipe-6-rollback
fun rollbackConfig() {
    val success = AppFeatures.rollback(steps = 1)
    if (!success) RecipeLogger.warn { "Rollback failed: insufficient history" }
}
// endregion recipe-6-rollback

// region recipe-7-shadow
fun evaluateWithShadowedConfig(context: Context): Boolean {
    val candidateJson = fetchCandidateConfig()
    val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()
    val candidateRegistry =
        InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
            load(candidateConfig)
        }

    val value =
        AppFeatures.darkMode.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            onMismatch = { mismatch ->
                RecipeLogger.warn {
                    "shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value}"
                }
            },
        )

    return applyDarkMode(value)
}
// endregion recipe-7-shadow

// region recipe-8-namespace
sealed class AppDomain(id: String) : Namespace(id) {
    data object Payments : AppDomain("payments") {
        val applePay by boolean<Context>(default = false)
    }

    data object Search : AppDomain("search") {
        val reranker by boolean<Context>(default = false)
    }
}

fun disablePayments() {
    AppDomain.Payments.disableAll()
}
// endregion recipe-8-namespace

// region recipe-9-observability
fun attachHooks() {
    val hooks =
        RegistryHooks.of(
            logger =
                object : KonditionalLogger {
                    override fun warn(message: () -> String, throwable: Throwable?) {
                        AppLogger.warn(message(), throwable)
                    }
                },
            metrics =
                object : MetricsCollector {
                    override fun recordEvaluation(event: Metrics.Evaluation) {
                        AppMetrics.increment("konditional.eval", tags = mapOf("feature" to event.featureKey))
                    }
                },
        )

    AppFeatures.setHooks(hooks)
}
// endregion recipe-9-observability
