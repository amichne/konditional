package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.definitionFor

/** One registry read shared by a feature and its same-namespace dependencies. */
internal class EvaluationSnapshot(val registry: NamespaceRegistry) {
    private val snapshot = registry.currentSnapshot
    val namespaceId get() = registry.namespaceId
    val version get() = snapshot.version
    val allDisabled get() = snapshot.allDisabled

    fun <T : Any, C : Context, M : Namespace<*>> flag(feature: Feature<T, C, M>): FlagDefinition<T, C, M> =
        snapshot.configuration.flags.definitionFor(feature)
            ?: (registry as? Namespace<*>)?.declaredDefinition(feature)
            ?: error("Flag not found for feature '${feature.key}' in namespace '$namespaceId'.")
}
