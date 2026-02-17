package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.ContextRuleScope
import io.amichne.konditional.core.dsl.rules.RuleScope
import io.amichne.konditional.core.dsl.rules.RuleSet
import io.amichne.konditional.core.dsl.rules.RuleSetBuilder
import io.amichne.konditional.core.features.Feature
import kotlin.reflect.KClass

/**
 * Semantic tokens for boolean values in DSL contexts.
 */
const val ENABLED: Boolean = true
const val DISABLED: Boolean = false

/**
 * Defines a boolean rule that yields `true` when the criteria matches.
 *
 * This is syntactic sugar for `rule(true) { ... }`.
 *
 * @param build DSL block for configuring targeting criteria
 */
fun <C : Context, M : Namespace> FlagScope<Boolean, C, M>.enable(build: RuleScope<C>.() -> Unit = {}) =
    rule(ENABLED, build)

/**
 * Defines a boolean rule that yields `false` when the criteria matches.
 *
 * This is syntactic sugar for `rule(false) { ... }`.
 *
 * @param build DSL block for configuring targeting criteria
 */
fun <C : Context, M : Namespace> FlagScope<Boolean, C, M>.disable(build: RuleScope<C>.() -> Unit = {}) =
    rule(DISABLED, build)

/**
 * Defines a boolean rule that yields `true` using a composable rule scope.
 *
 * This is syntactic sugar for `ruleScoped(true) { ... }`.
 *
 * @param build DSL block for configuring composable targeting criteria
 */
fun <C : Context, M : Namespace> FlagScope<Boolean, C, M>.enableScoped(build: ContextRuleScope<C>.() -> Unit = {}) =
    ruleScoped(ENABLED, build)

/**
 * Defines a boolean rule that yields `false` using a composable rule scope.
 *
 * This is syntactic sugar for `ruleScoped(false) { ... }`.
 *
 * @param build DSL block for configuring composable targeting criteria
 */
fun <C : Context, M : Namespace> FlagScope<Boolean, C, M>.disableScoped(build: ContextRuleScope<C>.() -> Unit = {}) =
    ruleScoped(DISABLED, build)

/**
 * Builds a rule set scoped to this feature using the feature's declared context type.
 *
 * This overload keeps call sites minimal when you do not need a contravariant
 * context type. The compiler infers all types from the feature receiver.
 */
@JvmName("ruleSetDefault")
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.ruleSet(
    build: RuleSetBuilder<T, C>.() -> Unit,
): RuleSet<C, T, C, M> =
    RuleSet(feature = this, rules = RuleSetBuilder<T, C>(axisCatalog = namespace.axisCatalog).apply(build).build())

/**
 * Builds a rule set using an explicit supertype context without reified generics.
 *
 * Use this when you want contravariant rule sets but prefer value-based
 * type selection at the call site:
 * ```kotlin
 * val global = feature.ruleSet(Context::class) { rule(value) { ios() } }
 * ```
 */
@JvmName("ruleSetWithContextType")
fun <T : Any, C, M : Namespace, RC : Context> Feature<T, C, M>.ruleSet(
    @Suppress("UNUSED_PARAMETER") contextType: KClass<RC>,
    build: RuleSetBuilder<T, RC>.() -> Unit,
): RuleSet<RC, T, C, M> where C : RC =
    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>(axisCatalog = namespace.axisCatalog).apply(build).build())

/**
 * Builds a rule set using a reified supertype context.
 *
 * Prefer this when you want contravariant context composition and a terse
 * call site:
 * ```kotlin
 * val global = feature.ruleSet<Context> { rule(value) { ios() } }
 * ```
 */
inline fun <reified RC : Context, T : Any, C, M : Namespace> Feature<T, C, M>.ruleSet(
    build: RuleSetBuilder<T, RC>.() -> Unit,
): RuleSet<RC, T, C, M> where C : RC =
    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>(axisCatalog = namespace.axisCatalog).apply(build).build())

/**
 * Type-based value setter using a scoped axis catalog.
 *
 * This extension allows setting values by type without explicitly passing the axis:
 * ```kotlin
 * axisValues {
 *     axis(Environment.PROD)  // Axis inferred from type
 * }
 * ```
 *
 * Requires that [T]'s axis has already been declared in the [AxisCatalogScope] attached
 * to this builder (for example via `axisValues(axisCatalog) { ... }`).
 *
 * @param value The value to set
 */
fun <T> AxisValuesScope.axis(value: T) where T : AxisValue<T>, T : Enum<T> {
    val catalog = (this as? AxisCatalogScope)?.axisCatalog
        ?: throw IllegalArgumentException(
            "Type-inferred axis(value) requires a scoped AxisCatalog. " +
                "Use axisValues(axisCatalog) { ... } or axis(axisHandle, value).",
        )
    axis(catalog.axisForOrThrow(value::class), value)
}

/**
 * Explicit axis setter for one or more values.
 *
 * Use this overload when you want axis resolution to be explicit and local.
 */
fun <T> AxisValuesScope.axis(
    axis: Axis<T>,
    vararg values: T,
) where T : AxisValue<T>, T : Enum<T> {
    values.forEach { set(axis, it) }
}

context(scope: AxisValuesScope)
operator fun <T> T.unaryPlus() where T : AxisValue<T>, T : Enum<T> = scope.axis(this)
