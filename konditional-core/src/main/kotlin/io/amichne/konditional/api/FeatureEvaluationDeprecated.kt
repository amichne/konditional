@file:kotlin.jvm.JvmName("FeatureEvaluationKt")
@file:kotlin.jvm.JvmMultifileClass

package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistry

@Deprecated(
    message = "Use explain() instead for clearer intent",
    replaceWith = ReplaceWith("explain(context, registry)"),
    level = DeprecationLevel.WARNING,
)
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T> = explain(context, registry)
