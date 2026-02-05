@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.NamespaceRegistry

/**
 * Evaluates this feature against the baseline registry (returned) and shadow-evaluates against [candidateRegistry].
 *
 * This is intended for gradual migrations between two flag systems or two configurations:
 * - callers use the baseline value for behavior
 * - the candidate evaluation is used only for comparison / alerting
 *
 * By default, candidate evaluation is skipped when the baseline registry kill-switch is enabled.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {},
): T {
    val baseline = evaluateInternalApi(context, baselineRegistry, mode = Metrics.Evaluation.EvaluationMode.NORMAL)

    if (baselineRegistry.isAllDisabled && !options.evaluateCandidateWhenBaselineDisabled) {
        return baseline.value
    }

    val candidate = evaluateInternalApi(context, candidateRegistry, mode = Metrics.Evaluation.EvaluationMode.SHADOW)
    val mismatchKinds = buildSet {
        if (baseline.value != candidate.value) add(ShadowMismatch.Kind.VALUE)
        if (options.reportDecisionMismatches && baseline.decision::class != candidate.decision::class) {
            add(ShadowMismatch.Kind.DECISION)
        }
    }

    if (mismatchKinds.isNotEmpty()) {
        onMismatch(
            ShadowMismatch(
                featureKey = key,
                baseline = baseline,
                candidate = candidate,
                kinds = mismatchKinds,
            )
        )
        baselineRegistry.hooks.logger.warn(
            message = {
                "konditional.shadowMismatch namespaceId=${baseline.namespaceId} key=$key kinds=$mismatchKinds baselineVersion=${baseline.configVersion} candidateVersion=${candidate.configVersion}"
            },
            throwable = null,
        )
    }

    return baseline.value
}

/**
 * Shadow-evaluates this feature without returning a value.
 *
 * This is useful for "dark launches" where you want comparison telemetry without coupling the call site
 * to the baseline return value.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {},
) {
    evaluateWithShadow(
        context = context,
        candidateRegistry = candidateRegistry,
        baselineRegistry = baselineRegistry,
        options = options,
        onMismatch = onMismatch,
    )
}
