package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.rules.targeting.scopes.AxisTargetingScope

/**
 * DSL scope for selecting one or more values for a specific [Axis] in a [variant] block.
 *
 * Invariants:
 * - At least one value must be selected per axis block.
 * - Repeated values are de-duplicated with deterministic insertion order.
 *
 * Example:
 * ```kotlin
 * variant {
 *     Axes.Environment {
 *         include(Environment.PROD, Environment.STAGE)
 *     }
 * }
 * ```
 */
@KonditionalDsl
interface AxisSelectionScope<V> where V : Enum<V>, V : AxisValue<V> {
    /**
     * Includes one or more values for the current axis selection.
     *
     * The first argument guarantees non-empty selection at compile time.
     */
    fun include(
        first: V,
        vararg rest: V,
    )
}

@PublishedApi
internal interface VariantDispatchHost {
    fun <V> onAxisSelection(
        axis: Axis<V>,
        values: Set<V>,
    ) where V : AxisValue<V>, V : Enum<V>
}

/**
 * DSL receiver for `variant { ... }` blocks used by both targeting and context axis builders.
 *
 * Semantics:
 * - Each `Axis.invoke { ... }` block contributes one axis constraint/assignment.
 * - Rule-side builders merge repeated calls for the same axis as OR-within-axis.
 * - Context-side builders accumulate selected values per axis.
 *
 * Error semantics:
 * - An empty axis block (for example `Axes.Environment { }`) throws [IllegalArgumentException].
 *
 * Migration:
 * - Replaces `axis(...)` and `set(...)`/`setIfNotNull(...)` style APIs.
 */
@KonditionalDsl
class VariantScope @PublishedApi internal constructor(
    private val host: VariantDispatchHost,
) {
    /**
     * Selects values for this [Axis] within a [variant] block.
     */
    operator fun <V> Axis<V>.invoke(
        block: AxisSelectionScope<V>.() -> Unit,
    ) where V : AxisValue<V>, V : Enum<V> {
        val selection = AxisSelectionScopeImpl<V>().apply(block)
        require(selection.selectedValues.isNotEmpty()) {
            "Axis selection for '${id}' must include at least one value."
        }
        host.onAxisSelection(this, selection.selectedValues)
    }
}

private class AxisSelectionScopeImpl<V> : AxisSelectionScope<V> where V : Enum<V>, V : AxisValue<V> {
    val selectedValues = linkedSetOf<V>()

    override fun include(
        first: V,
        vararg rest: V,
    ) {
        selectedValues += first
        selectedValues += rest
    }
}

/**
 * Legacy axis-targeting DSL entry point.
 *
 * @deprecated Use [constrain] instead for clearer, more concise axis constraints.
 * Replace `variant { axisHandle { include(...) } }` with `constrain(...)`.
 */
@Deprecated(
    message = "Use constrain(...) for axis targeting." +
        "Replace variant { axisHandle { include(...) } } with constrain(...).",
    replaceWith = ReplaceWith("constrain(/* axis values */)"),
    level = DeprecationLevel.ERROR,
)
fun AxisTargetingScope<*>.variant(
    block: VariantScope.() -> Unit,
) {
    val host = this as? VariantDispatchHost
        ?: error("Unsupported AxisTargetingScope receiver for variant DSL: ${this::class.qualifiedName}")
    VariantScope(host).apply(block)
}
