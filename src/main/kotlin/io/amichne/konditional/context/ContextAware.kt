package io.amichne.konditional.context

//import io.amichne.konditional.core.Namespace.Authentication.flag

fun interface ContextAware<out C : Context> {
    fun factory(): C
    val context: C get() = factory()

    fun interface Factory<out C : Context> {
        fun context(): C
    }
}
