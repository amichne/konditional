file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/BasePredicate.kt
package=io.amichne.konditional.rules.evaluable
imports=io.amichne.konditional.context.Context,io.amichne.konditional.context.Context.Companion.getAxisValue,io.amichne.konditional.context.Context.LocaleContext,io.amichne.konditional.context.Context.PlatformContext,io.amichne.konditional.context.Context.VersionContext,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.rules.evaluable.BasePredicate|kind=class|decl=internal data class BasePredicate<C : Context>( val locales: Set<String> = emptySet(), val platforms: Set<String> = emptySet(), val versionRange: VersionRange = Unbounded, val axisConstraints: List<AxisConstraint> = emptyList(), ) : Predicate<C>
methods:
- override fun matches(context: C): Boolean
- override fun specificity(): Int
