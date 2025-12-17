package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.values.FeatureId

interface Identifiable {
    val id: FeatureId

    companion object {
        operator fun <M : Namespace> invoke(
            key: String,
            namespace: M,
        ): Identifiable =
            object : Identifiable {
                override val id: FeatureId = FeatureId(namespaceSeed = namespace.identifierSeed, key = key)
            }
    }
}
