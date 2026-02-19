file=konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleScope.kt
package=io.amichne.konditional.core.dsl.rules
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.FlagScope,io.amichne.konditional.core.dsl.KonditionalDsl,io.amichne.konditional.core.dsl.rules.targeting.scopes.LocaleTargetingScope,io.amichne.konditional.core.dsl.rules.targeting.scopes.PlatformTargetingScope,io.amichne.konditional.core.dsl.rules.targeting.scopes.StableIdTargetingScope,io.amichne.konditional.core.dsl.rules.targeting.scopes.VersionTargetingScope
type=io.amichne.konditional.core.dsl.rules.RuleScope|kind=interface|decl=interface RuleScope<C : Context> : ContextRuleScope<C>,
type=io.amichne.konditional.core.dsl.rules.Prefix|kind=class|decl=class Prefix<T : Any, C : Context, out M : Namespace> internal constructor( private val scope: FlagScope<T, C, @UnsafeVariance M>, private val build: RuleScope<C>.() -> Unit, )
type=io.amichne.konditional.core.dsl.rules.ScopedPrefix|kind=class|decl=class ScopedPrefix<T : Any, C : Context, out M : Namespace> internal constructor( private val scope: FlagScope<T, C, @UnsafeVariance M>, private val build: ContextRuleScope<C>.() -> Unit, )
type=io.amichne.konditional.core.dsl.rules.Postfix|kind=object|decl=object Postfix
methods:
- infix fun yields(value: T): Postfix
