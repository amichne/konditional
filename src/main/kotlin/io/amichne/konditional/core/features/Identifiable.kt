package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.values.Identifier

interface Identifiable {
    val id: Identifier

    companion object {
        operator fun <M : Namespace> invoke(
            key: String,
            namespace: M,
        ): Identifiable = object : Identifiable {
            override val id: Identifier = Identifier(namespace.uuid.toString(), key)
        }
    }
}
