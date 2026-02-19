file=konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/EnterpriseTestFeatures.kt
package=io.amichne.konditional.fixtures
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.id.StableId,io.amichne.konditional.rules.targeting.Targeting
type=io.amichne.konditional.fixtures.SubscriptionTier|kind=enum|decl=enum class SubscriptionTier
type=io.amichne.konditional.fixtures.UserRole|kind=enum|decl=enum class UserRole
type=io.amichne.konditional.fixtures.EnterpriseContext|kind=class|decl=data class EnterpriseContext( override val locale: AppLocale, override val platform: Platform, override val appVersion: Version, override val stableId: StableId, val organizationId: String, val subscriptionTier: SubscriptionTier, val userRole: UserRole, ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext
type=io.amichne.konditional.fixtures.CompositeContext|kind=class|decl=data class CompositeContext( val context: Context, val experimentGroups: Set<String>, val sessionId: String, ) : Context,
type=io.amichne.konditional.fixtures.ExperimentContext|kind=class|decl=data class ExperimentContext( override val locale: AppLocale, override val platform: Platform, override val appVersion: Version, override val stableId: StableId, val experimentGroups: Set<String>, val sessionId: String, ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext
type=io.amichne.konditional.fixtures.EnterpriseFeatures|kind=object|decl=object EnterpriseFeatures : Namespace.TestNamespaceFacade("enterprise-features")
type=io.amichne.konditional.fixtures.ExperimentFeatures|kind=object|decl=object ExperimentFeatures : Namespace.TestNamespaceFacade("experiment-features")
fields:
- val advanced_analytics by boolean<EnterpriseContext>(default = false)
- val custom_branding by boolean<CompositeContext>(default = false)
- val api_access by boolean<EnterpriseContext>(default = false)
- val homepage_variant by string<ExperimentContext>(default = "default")
- val onboarding_style by string<ExperimentContext>(default = "test")
