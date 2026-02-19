file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/targeting/Targeting.kt
package=io.amichne.konditional.rules.targeting
imports=io.amichne.konditional.context.Context,io.amichne.konditional.context.Context.Companion.getAxisValue,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.rules.targeting.Targeting|kind=interface|decl=sealed interface Targeting<in C : Context>
type=io.amichne.konditional.rules.targeting.Locale|kind=class|decl=value class Locale(val ids: Set<String>) : Targeting<Context.LocaleContext>
type=io.amichne.konditional.rules.targeting.Platform|kind=class|decl=value class Platform(val ids: Set<String>) : Targeting<Context.PlatformContext>
type=io.amichne.konditional.rules.targeting.Version|kind=class|decl=data class Version(val range: VersionRange) : Targeting<Context.VersionContext>
type=io.amichne.konditional.rules.targeting.Axis|kind=class|decl=data class Axis( val axisId: String, val allowedIds: Set<String>, ) : Targeting<Context>
type=io.amichne.konditional.rules.targeting.Custom|kind=class|decl=data class Custom<C : Context>( val block: (C) -> Boolean, val weight: Int = 1, ) : Targeting<C>
type=io.amichne.konditional.rules.targeting.Guarded|kind=class|decl=data class Guarded<C : Context, R : Context>( val inner: Targeting<R>, val evidence: (C) -> R?, ) : Targeting<C>
type=io.amichne.konditional.rules.targeting.All|kind=class|decl=data class All<C : Context>( val targets: List<Targeting<C>>, ) : Targeting<C>
methods:
- fun matches(context: C): Boolean fun specificity(): Int @JvmInline value class Locale(val ids: Set<String>) : Targeting<Context.LocaleContext>
- override fun matches(context: Context.LocaleContext): Boolean
- override fun specificity(): Int
- override fun matches(context: Context.PlatformContext): Boolean
- override fun specificity(): Int
- override fun matches(context: Context.VersionContext): Boolean
- override fun specificity(): Int
- override fun matches(context: Context): Boolean
- override fun specificity(): Int
- override fun matches(context: C): Boolean
- override fun specificity(): Int
- override fun matches(context: C): Boolean
- override fun specificity(): Int
- override fun matches(context: C): Boolean
- override fun specificity(): Int
- operator fun plus(other: All<C>): All<C>
