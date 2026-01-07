@file:OptIn(io.amichne.konditional.internal.KonditionalInternalApi::class)

package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.internal.KonditionalInternalApi

/**
 * Semantic tokens for boolean values in DSL contexts.
 */
const val ENABLED: Boolean = true
const val DISABLED: Boolean = false

/**
 * DSL wrapper representing a partially-specified rule: criteria first, value second.
 *
 * Created via [FlagScope.rule] and completed via [yields].
 */
@KonditionalDsl
class YieldingScope<T : Any, C : Context> internal constructor(
    private val scope: FlagScope<T, C>,
    private val build: RuleScope<C>.() -> Unit,
) {
    private val host: YieldingScopeHost? = scope as? YieldingScopeHost
    private val pendingToken: PendingYieldToken = PendingYieldToken(callSite = captureRuleCallSite()).also {
        host?.registerPendingYield(it)
    }

    /**
     * Completes the rule declaration by assigning the value to yield when the criteria matches.
     */
    infix fun yields(value: T) =
        host?.commitYield(pendingToken) { scope.rule(value, build) } ?: scope.rule(value, build)
}

@KonditionalInternalApi
interface YieldingScopeHost {
    fun registerPendingYield(token: PendingYieldToken)

    fun commitYield(token: PendingYieldToken, commit: () -> Unit)
}

@KonditionalInternalApi
class PendingYieldToken internal constructor(
    val callSite: String?,
)

private fun captureRuleCallSite(): String? =
    Throwable("Rule call site capture")
        .stackTrace
        .asSequence()
        .dropWhile { it.className.startsWith("io.amichne.konditional.core.dsl.") }
        .dropWhile { it.className.startsWith("io.amichne.konditional.internal.builders.") }
        .firstOrNull()
        ?.let { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }

/**
 * Boolean sugar for rule declaration. Only available for boolean feature builders.
 *
 * Semantics:
 * - `enable { ... }`  ≡ `rule(true) { ... }`
 * - `disable { ... }` ≡ `rule(false) { ... }`
 */
fun <C : Context> FlagScope<Boolean, C>.enable(build: RuleScope<C>.() -> Unit = {}) =
    rule(ENABLED, build)

fun <C : Context> FlagScope<Boolean, C>.disable(build: RuleScope<C>.() -> Unit = {}) =
    rule(DISABLED, build)
