@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.rules.targeting.scopes.LocaleTargetingScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.PlatformTargetingScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.StableIdTargetingScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.VersionTargetingScope
import io.amichne.konditional.rules.RuleValue

/**
 * DSL scope for rule configuration.
 *
 * This interface defines the public API for configuring targeting rules.
 * Users cannot instantiate implementations of this interface directly - it is only
 * available as a receiver in DSL blocks through internal implementations.
 *
 * Example usage:
 * ```kotlin
 * rule {
 *     locales(AppLocale.UNITED_STATES, AppLocale.CANADA)
 *     platforms(Platform.IOS, Platform.ANDROID)
 *     versions {
 *         min(1, 2, 0)
 *         max(2, 0, 0)
 *     }
 *     rampUp {  RampUp.create(50.0) }
 *     note("RampUp to mobile users only")
 * }
 * ```
 *
 * @param C The contextFn type the rule evaluates against
 * @since 0.0.2
 */
@KonditionalDsl
interface RuleScope<C : Context> : ContextRuleScope<C>,
                                   LocaleTargetingScope<C>,
                                   PlatformTargetingScope<C>,
                                   VersionTargetingScope<C>,
                                   StableIdTargetingScope<C> {

    companion object {
        private fun captureRuleCallSite(): String? =
            Throwable("Rule call site capture")
                .stackTrace
                .asSequence()
                .dropWhile { it.className.startsWith("io.amichne.konditional.core.dsl.") }
                .dropWhile { it.className.startsWith("io.amichne.konditional.internal.builders.") }
                .firstOrNull()
                ?.let { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
    }

    /**
     * DSL wrapper representing a partially-specified rule: criteria first, value second.
     *
     * Created via [io.amichne.konditional.core.dsl.FlagScope.rule] and completed via [yields].
     */
    @KonditionalDsl
    class Prefix<T : Any, C : Context, out M : Namespace> internal constructor(
        private val scope: FlagScope<T, C, @UnsafeVariance M>,
        private val build: RuleScope<C>.() -> Unit,
    ) {
        private val host: YieldingScopeHost? = scope as? YieldingScopeHost
        private val pendingToken: PendingYieldToken =
            PendingYieldToken(callSite = captureRuleCallSite()).also { host?.registerPendingYield(it) }

        /**
         * Completes the rule declaration by assigning the value to yield when the criteria matches.
         *
         * Semantics:
         * `rule { criteria } yields VALUE` ≡ `rule(VALUE) { criteria }`
         *
         * When invoked from the criteria-first DSL, this also closes the pending rule
         * and makes it eligible for validation during flag construction.
         */
        infix fun yields(value: T): Postfix = host
            ?.commitYield(pendingToken) { scope.rule(value, build) }
            ?.let { Postfix }
            ?: run {
                scope.rule(value, build)
                Postfix
            }

        /**
         * Completes the rule declaration by assigning a context-derived value when the criteria matches.
         *
         * Semantics:
         * `rule { criteria } yields { value }` ≡ `rule({ value }) { criteria }`
         */
        infix fun yields(value: C.() -> T): Postfix = host
            ?.commitYield(pendingToken) { scope.ruleValue(RuleValue.contextual(value), build) }
            ?.let { Postfix }
            ?: run {
                scope.ruleValue(RuleValue.contextual(value), build)
                Postfix
            }
    }

    /**
     * DSL wrapper representing a partially-specified rule: criteria first, value second,
     * using a composable scope.
     *
     * Created via [FlagScope.ruleScoped] and completed via [yields].
     */
    @KonditionalDsl
    class ScopedPrefix<T : Any, C : Context, out M : Namespace> internal constructor(
        private val scope: FlagScope<T, C, @UnsafeVariance M>,
        private val build: ContextRuleScope<C>.() -> Unit,
    ) {
        private val host: YieldingScopeHost? = scope as? YieldingScopeHost
        private val pendingToken: PendingYieldToken =
            PendingYieldToken(callSite = captureRuleCallSite()).also { host?.registerPendingYield(it) }

        /**
         * Completes the rule declaration by assigning the value to yield when the criteria matches.
         *
         * Semantics:
         * `ruleScoped { criteria } yields VALUE` ≡ `ruleScoped(VALUE) { criteria }`
         *
         * When invoked from the criteria-first DSL, this also closes the pending rule
         * and makes it eligible for validation during flag construction.
         */
        infix fun yields(value: T): Postfix = host
            ?.commitYield(pendingToken) { scope.ruleScoped(value, build) }
            ?.let { Postfix }
            ?: run {
                scope.ruleScoped(value, build)
                Postfix
            }

        /**
         * Completes the rule declaration by assigning a context-derived value when the criteria matches.
         *
         * Semantics:
         * `ruleScoped { criteria } yields { value }` ≡ `ruleScoped({ value }) { criteria }`
         */
        infix fun yields(value: C.() -> T): Postfix = host
            ?.commitYield(pendingToken) { scope.ruleScopedValue(RuleValue.contextual(value), build) }
            ?.let { Postfix }
            ?: run {
                scope.ruleScopedValue(RuleValue.contextual(value), build)
                Postfix
            }
    }

    /**
     * Marker value returned after completing a criteria-first rule.
     */
    @KonditionalDsl
    object Postfix
}
