file=konditional-observability/src/docsSamples/kotlin/io/amichne/konditional/docsamples/RecipesSamples.kt
package=io.amichne.konditional.docsamples
imports=io.amichne.konditional.api.axisValues,io.amichne.konditional.api.evaluate,io.amichne.konditional.api.evaluateWithShadow,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.ops.KonditionalLogger,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.core.ops.MetricsCollector,io.amichne.konditional.core.ops.RegistryHooks,io.amichne.konditional.core.registry.InMemoryNamespaceRegistry,io.amichne.konditional.core.result.parseErrorOrNull,io.amichne.konditional.core.types.Konstrained,io.amichne.konditional.runtime.load,io.amichne.konditional.runtime.rollback,io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec,io.amichne.kontracts.dsl.of,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.schema.ObjectSchema
type=io.amichne.konditional.docsamples.AppFeatures|kind=object|decl=private object AppFeatures : Namespace("app")
type=io.amichne.konditional.docsamples.AppLogger|kind=object|decl=private object AppLogger
type=io.amichne.konditional.docsamples.AppMetrics|kind=object|decl=private object AppMetrics
type=io.amichne.konditional.docsamples.RecipeLogger|kind=object|decl=private object RecipeLogger
type=io.amichne.konditional.docsamples.CheckoutVariant|kind=enum|decl=enum class CheckoutVariant
type=io.amichne.konditional.docsamples.CheckoutFlags|kind=object|decl=object CheckoutFlags : Namespace("checkout")
type=io.amichne.konditional.docsamples.RampUpFlags|kind=object|decl=object RampUpFlags : Namespace("ramp-up")
type=io.amichne.konditional.docsamples.RampUpResetFlags|kind=object|decl=object RampUpResetFlags : Namespace("ramp-up-reset")
type=io.amichne.konditional.docsamples.Segment|kind=enum|decl=enum class Segment(override val id: String) : AxisValue<Segment>
type=io.amichne.konditional.docsamples.SegmentFlags|kind=object|decl=object SegmentFlags : Namespace("segment")
type=io.amichne.konditional.docsamples.EnterpriseContext|kind=class|decl=data class EnterpriseContext( override val locale: AppLocale, override val platform: Platform, override val appVersion: Version, override val stableId: StableId, val subscriptionTier: SubscriptionTier, val employeeCount: Int, ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext
type=io.amichne.konditional.docsamples.SubscriptionTier|kind=enum|decl=enum class SubscriptionTier
type=io.amichne.konditional.docsamples.PremiumFeatures|kind=object|decl=object PremiumFeatures : Namespace("premium")
type=io.amichne.konditional.docsamples.RetryPolicy|kind=class|decl=data class RetryPolicy( val maxAttempts: Int = 3, val backoffMs: Double = 1000.0, val enabled: Boolean = true, ) : Konstrained<ObjectSchema>
type=io.amichne.konditional.docsamples.PolicyFlags|kind=object|decl=object PolicyFlags : Namespace("policy")
type=io.amichne.konditional.docsamples.AppDomain|kind=class|decl=sealed class AppDomain(id: String) : Namespace(id)
type=io.amichne.konditional.docsamples.Payments|kind=object|decl=data object Payments : AppDomain("payments")
type=io.amichne.konditional.docsamples.Search|kind=object|decl=data object Search : AppDomain("search")
fields:
- val darkMode by boolean<Context>(default = false)
- val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
- val newCheckout by boolean<Context>(default = false)
- val newCheckout by boolean<Context>(default = false)
- val segmentAxis
- val premiumUi by boolean<Context>(default = false)
- val advancedAnalytics by boolean<EnterpriseContext>(default = false)
- override val schema
- val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy())
- val applePay by boolean<Context>(default = false)
- val reranker by boolean<Context>(default = false)
methods:
- fun warn(message: String, throwable: Throwable?)
- fun increment(name: String, tags: Map<String, String>)
- fun error(message: () -> String)
- fun warn(message: () -> String)
