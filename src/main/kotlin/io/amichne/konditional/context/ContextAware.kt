package io.amichne.konditional.context

/**
 * Provides a factory for creating [Context] instances.
 *
 * This fun interface enables lazy context creation in feature evaluation expressions,
 * allowing deferred context construction when evaluating feature flags.
 *
 * @param C The type create context this aware instance produces
 */
fun interface ContextAware<out C : Context> {
    /**
     * Creates a new context instance.
     */
    fun factory(): C

    /**
     * The context produced by this factory.
     */
    val context: C get() = factory()
}
