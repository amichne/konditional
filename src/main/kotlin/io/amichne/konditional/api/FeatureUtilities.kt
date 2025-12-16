package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.ContextAware
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.registry.NamespaceRegistry

inline fun <reified T : Any, reified C : Context, M : Namespace, D> D.feature(
    block: D.() -> Feature<T, C, M>,
): T where D : ContextAware<C>, D : FeatureAware<out M> = block().evaluate(context)

/**
 * Evaluate with an explicit context instance.
 */
inline fun <reified T : Any, reified C : Context, D> D.feature(
    context: C,
    @KonditionalDsl block: D.() -> Feature<T, C, *>,
): T where D : FeatureAware<*>, D : ContextAware<*> = block().evaluate(context)

/**
 * Evaluates this feature for the given contextFn.
 *
 * By default, evaluates using the feature's namespace registry.
 * For testing, you can provide an explicit registry parameter.
 *
 * @param context The evaluation contextFn
 * @param registry The registry to use (defaults to the feature's namespace)
 * @return The evaluated value, or null if the feature is not registered
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T = registry.flag(this).evaluate(context)
