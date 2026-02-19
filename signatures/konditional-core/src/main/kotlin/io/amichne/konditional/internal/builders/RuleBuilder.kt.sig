file=konditional-core/src/main/kotlin/io/amichne/konditional/internal/builders/RuleBuilder.kt
package=io.amichne.konditional.internal.builders
imports=io.amichne.konditional.context.Context,io.amichne.konditional.context.LocaleTag,io.amichne.konditional.context.PlatformTag,io.amichne.konditional.context.RampUp,io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.core.dsl.KonditionalDsl,io.amichne.konditional.core.dsl.VersionRangeScope,io.amichne.konditional.core.dsl.rules.RuleScope,io.amichne.konditional.core.id.HexId,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.registry.AxisCatalog,io.amichne.konditional.internal.builders.versions.VersionRangeBuilder,io.amichne.konditional.rules.Rule,io.amichne.konditional.rules.targeting.Targeting
type=io.amichne.konditional.internal.builders.RuleBuilder|kind=class|decl=internal class RuleBuilder<C : Context>( private val axisCatalog: AxisCatalog? = null, ) : RuleScope<C>
fields:
- private val leaves
- private var note: String?
- private var rampUp: RampUp?
- private val allowlist
methods:
- override fun locales(vararg appLocales: LocaleTag)
- override fun platforms(vararg ps: PlatformTag)
- override fun versions(build: VersionRangeScope.() -> Unit)
- override fun extension(block: C.() -> Boolean)
- override fun <T> axis( axis: Axis<T>, vararg values: T, ) where T : AxisValue<T>, T : Enum<T>
- override fun <T> axis(vararg values: T) where T : AxisValue<T>, T : Enum<T>
- override fun allowlist(vararg stableIds: StableId)
- override fun note(text: String)
- override fun rampUp(function: () -> Number)
