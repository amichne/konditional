package io.amichne.konditional.kontext

import io.amichne.konditional.core.Namespace

interface Konstrained<C : Kontext<out Namespace>> : DoublyAware<C, Namespace>{
    companion object {
        inline fun <C : Kontext<*>> kontext(
            crossinline kontextFactory: () -> C
        ): Konstrained<C> = object : Konstrained<C>() {
            override fun factory(): C = kontextFactory()
        }
    }
}
