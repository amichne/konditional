package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.internal.builders.RuleBuilder
import io.amichne.konditional.rules.Rule
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * A feature-scoped set of rules that can be composed with other rule sets.
 *
 * Rule sets are contravariant in context to allow composing contributors written
 * against supertypes of the feature's context type.
 */
@KonditionalDsl
data class RuleSpec<out T : Any, in C : Context>(
    val value: T,
    val rule: Rule<C>,
)

@KonditionalDsl
class RuleSet<RC : Context, T : Any, C, M : Namespace> @PublishedApi internal constructor(
    val feature: Feature<T, C, M>,
    internal val rules: List<RuleSpec<T, RC>>,
) where C : RC {
    operator fun plus(other: RuleSet<RC, T, C, M>): RuleSet<RC, T, C, M> =
        RuleSet(feature, rules + other.rules)

    companion object {
        fun <RC : Context, T : Any, C, M : Namespace> empty(
            feature: Feature<T, C, M>,
        ): RuleSet<RC, T, C, M> where C : RC =
            RuleSet(feature, emptyList())
    }
}

@KonditionalDsl
class RuleSetBuilder<T : Any, C : Context> @PublishedApi internal constructor() {
    private val rules = mutableListOf<RuleSpec<T, C>>()

    fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>().apply(build).build()
        rules += RuleSpec(value, rule)
    }

    fun ruleScoped(
        value: T,
        build: ContextRuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>().apply {
            @Suppress("UNCHECKED_CAST")
            (this as ContextRuleScope<C>).apply(build)
        }.build()
        rules += RuleSpec(value, rule)
    }

    @PublishedApi
    internal fun build(): List<RuleSpec<T, C>> = rules.toList()
}

@JvmName("ruleSetDefault")
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.ruleSet(
    build: RuleSetBuilder<T, C>.() -> Unit,
): RuleSet<C, T, C, M> =
    RuleSet(feature = this, rules = RuleSetBuilder<T, C>().apply(build).build())

@JvmName("ruleSetWithContextType")
fun <T : Any, C, M : Namespace, RC : Context> Feature<T, C, M>.ruleSet(
    @Suppress("UNUSED_PARAMETER") contextType: KClass<RC>,
    build: RuleSetBuilder<T, RC>.() -> Unit,
): RuleSet<RC, T, C, M> where C : RC =
    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>().apply(build).build())

inline fun <reified RC : Context, T : Any, C, M : Namespace> Feature<T, C, M>.ruleSet(
    build: RuleSetBuilder<T, RC>.() -> Unit,
): RuleSet<RC, T, C, M> where C : RC =
    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>().apply(build).build())
