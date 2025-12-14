package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

interface Identifiable {
    val id: String

    companion object {
        operator fun <M : Namespace> invoke(
            key: String,
            namespace: M,
        ): Identifiable = object : Identifiable {
            override val id: String
                get() = "${namespace.id}::$key"
        }
    }
}
