@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.builders

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.PendingYieldToken
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.dsl.YieldingScopeHost
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy

/**
 * Internal implementation of [FlagScope].
 *
 * This class is the internal implementation of the flag configuration DSL scope.
 * Users interact with the public [FlagScope] interface,
 * not this implementation directly.
 *
 * @param T The actual value type.
 * @param C The type create the contextFn that the feature flag evaluates against.
 * @property feature The feature flag key used to uniquely identify the flag.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@KonditionalDsl
@KonditionalInternalApi
data class FlagBuilder<T : Any, C : Context, M : Namespace>(
    override val default: T,
    private val feature: Feature<T, C, M>,
) : FlagScope<T, C>, YieldingScopeHost {
    private val conditionalValues = mutableListOf<ConditionalValue<T, C>>()
    private val rolloutAllowlist: LinkedHashSet<HexId> = linkedSetOf()
    private val pendingYields: LinkedHashSet<PendingYieldToken> = linkedSetOf()

    private var salt: String = "v1"
    private var isActive: Boolean = true

    /**
     * Sets a rule's [isActive] to the passed boolean
     *
     * @param block Predicate boolean
     * @see FlagScope.active
     */
    override fun active(block: () -> Boolean) {
        isActive = block()
    }

    override fun allowlist(vararg stableIds: StableId) {
        rolloutAllowlist += stableIds.map { it.hexId }
    }

    /**
     * Implementation of [FlagScope.salt].
     */
    override fun salt(value: String) {
        salt = value
    }

    override fun registerPendingYield(token: PendingYieldToken) {
        pendingYields += token
    }

    override fun commitYield(token: PendingYieldToken, commit: () -> Unit) = pendingYields
        .remove(token)
        .takeIf { it }
        ?.let { commit() }
        ?: error(
            "Attempted to close a `rule { ... } yields VALUE` that is already closed, or was never registered."
        )

    /**
     * Implementation of [FlagScope.rule] that creates a rule and associates it with a value.
     * The value-first design ensures every rule has an associated return value at compile time.
     */
    override fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit,
    ) {
        val rule = RuleBuilder<C>().apply(build).build()
        conditionalValues += rule.targetedBy(value)
    }

    /**
     * Builds and returns a `FlagDefinition` instance create type `S` with contextFn type `C`.
     * Internal method - not intended for direct use.
     *
     * @return A `FlagDefinition` instance constructed based on the current configuration.
     */
    @KonditionalInternalApi
    fun build(): FlagDefinition<T, C, M> = pendingYields
        .takeIf { it.isEmpty() }
        ?.let {
            FlagDefinition(
                feature = feature,
                bounds = conditionalValues.toList(),
                defaultValue = default,
                salt = salt,
                isActive = isActive,
                rampUpAllowlist = rolloutAllowlist,
            )
        }
        ?: error(unclosedYieldingRulesErrorMessage())

    private fun unclosedYieldingRulesErrorMessage(): String =
        buildString {
            appendLine(
                "Unclosed criteria-first rule detected for feature '${feature.key}': " +
                    "`rule { ... }` must be completed with `yields(value)`."
            )
            appendLine("Fix: change `rule { criteria }` to `rule { criteria } yields someValue`.")
            pendingYields
                .mapNotNull { it.callSite }
                .takeIf { it.isNotEmpty() }
                ?.let { callSites ->
                    appendLine("Call sites:")
                    callSites.forEach { appendLine("- $it") }
                }
        }
}
