@file:OptIn(KonditionalInternalApi::class)
@file:Suppress("TooManyFunctions")

package io.amichne.konditional.core.dsl

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.ContextRuleScope
import io.amichne.konditional.core.dsl.rules.NamespaceRuleSet
import io.amichne.konditional.core.dsl.rules.RuleScope
import io.amichne.konditional.core.dsl.rules.RuleSet
import io.amichne.konditional.core.dsl.rules.RuleSetBuilder
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.values.RuleId
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
@KonditionalInternalApi
fun <C : Context, M : Namespace> FlagScope<Boolean, C, M>.enableScoped(build: ContextRuleScope<C>.() -> Unit = {}) =
    ruleScoped(ENABLED, build)

/**
 * Defines a boolean rule that yields `false` using a composable rule scope.
 *
 * This is syntactic sugar for `ruleScoped(false) { ... }`.
 *
 * @param build DSL block for configuring composable targeting criteria
 */
@KonditionalInternalApi
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
    RuleSet(
        feature = this,
        rules = RuleSetBuilder<T, C>(
            ruleIdFactory = { ruleOrdinal -> RuleId.forFeatureRuleSetRule(id, ruleOrdinal) },
            namespaceId = namespace.id,
            predicateResolver = { ref -> namespace.predicates<C>().resolve(ref) },
            predicateRegistrar = { ref, predicate -> namespace.predicates<C>().registerOrReplace(ref, predicate) },
        ).apply(build).build(),
    )

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
    RuleSet(
        feature = this,
        rules = RuleSetBuilder<T, RC>(
            ruleIdFactory = { ruleOrdinal -> RuleId.forFeatureRuleSetRule(id, ruleOrdinal) },
            namespaceId = namespace.id,
            predicateResolver = { ref -> namespace.predicates<RC>().resolve(ref) },
            predicateRegistrar = { ref, predicate -> namespace.predicates<RC>().registerOrReplace(ref, predicate) },
        ).apply(build).build(),
    )

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
    RuleSet(
        feature = this,
        rules = RuleSetBuilder<T, RC>(
            ruleIdFactory = { ruleOrdinal -> RuleId.forFeatureRuleSetRule(id, ruleOrdinal) },
            namespaceId = namespace.id,
            predicateResolver = { ref -> namespace.predicates<RC>().resolve(ref) },
            predicateRegistrar = { ref, predicate -> namespace.predicates<RC>().registerOrReplace(ref, predicate) },
        ).apply(build).build(),
    )

/**
 * Builds a namespace-scoped rule set using an explicit value type.
 *
 * This variant is not bound to a specific feature and can be included by multiple features in
 * the same namespace.
 *
 * @param name Stable logical name for this rule set. This seed must stay stable across refactors
 *   to keep generated [RuleId] values deterministic.
 */
@JvmName("namespaceRuleSetDefault")
inline fun <reified T : Any, C : Context, M : Namespace> M.ruleSet(
    name: String,
    build: RuleSetBuilder<T, C>.() -> Unit,
): NamespaceRuleSet<C, T, C, M> =
    NamespaceRuleSet(
        namespace = this,
        rules = RuleSetBuilder<T, C>(
            ruleIdFactory = { ruleOrdinal -> RuleId.forNamespaceRuleSetRule(id, name, ruleOrdinal) },
            namespaceId = id,
            predicateResolver = { ref -> predicates<C>().resolve(ref) },
            predicateRegistrar = { ref, predicate -> predicates<C>().registerOrReplace(ref, predicate) },
        ).apply(build).build(),
    )

/**
 * Builds a namespace-scoped rule set using an explicit supertype context.
 *
 * @param name Stable logical name for this rule set. This seed must stay stable across refactors
 *   to keep generated [RuleId] values deterministic.
 */
@JvmName("namespaceRuleSetWithContextType")
inline fun <reified T : Any, C, M : Namespace, RC : Context> M.ruleSet(
    name: String,
    @Suppress("UNUSED_PARAMETER") contextType: KClass<RC>,
    build: RuleSetBuilder<T, RC>.() -> Unit,
): NamespaceRuleSet<RC, T, C, M> where C : RC =
    NamespaceRuleSet(
        namespace = this,
        rules = RuleSetBuilder<T, RC>(
            ruleIdFactory = { ruleOrdinal -> RuleId.forNamespaceRuleSetRule(id, name, ruleOrdinal) },
            namespaceId = id,
            predicateResolver = { ref -> predicates<RC>().resolve(ref) },
            predicateRegistrar = { ref, predicate -> predicates<RC>().registerOrReplace(ref, predicate) },
        ).apply(build).build(),
    )

/**
 * Builds a namespace-scoped rule set using reified value and context supertypes.
 *
 * @param name Stable logical name for this rule set. This seed must stay stable across refactors
 *   to keep generated [RuleId] values deterministic.
 */
inline fun <reified T : Any, reified RC : Context, C, M : Namespace> M.ruleSet(
    name: String,
    build: RuleSetBuilder<T, RC>.() -> Unit,
): NamespaceRuleSet<RC, T, C, M> where C : RC =
    NamespaceRuleSet(
        namespace = this,
        rules = RuleSetBuilder<T, RC>(
            ruleIdFactory = { ruleOrdinal -> RuleId.forNamespaceRuleSetRule(id, name, ruleOrdinal) },
            namespaceId = id,
            predicateResolver = { ref -> predicates<RC>().resolve(ref) },
            predicateRegistrar = { ref, predicate -> predicates<RC>().registerOrReplace(ref, predicate) },
        ).apply(build).build(),
    )
