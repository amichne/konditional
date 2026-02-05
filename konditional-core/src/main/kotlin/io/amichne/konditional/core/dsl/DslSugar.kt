package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.ContextRuleScope
import io.amichne.konditional.core.dsl.rules.RuleScope
import io.amichne.konditional.core.dsl.rules.RuleSet
import io.amichne.konditional.core.dsl.rules.RuleSetBuilder
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.AxisRegistry
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
    RuleSet(feature = this, rules = RuleSetBuilder<T, C>().apply(build).build())

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
    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>().apply(build).build())

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
    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>().apply(build).build())

/**
 * Type-based value setter using the registry.
 *
 * This extension allows setting values by type without explicitly passing the axis:
 * ```kotlin
 * axisValues {
 *     axis(Environment.PROD)  // Axis inferred from type
 * }
 * ```
 *
 * @param value The value to set
 * If no axis is registered for type T, an implicit axis is created using the
 * value type's qualified name as its id.
 */
inline fun <reified T> AxisValuesScope.axis(value: T) where T : AxisValue<T>, T : Enum<T> {
    AxisRegistry.axisForOrRegister(T::class).let { set(it, value) }
}

context(scope: AxisValuesScope)
inline operator fun <reified T> T.unaryPlus() where T : AxisValue<T>, T : Enum<T> = scope.axis(this)
