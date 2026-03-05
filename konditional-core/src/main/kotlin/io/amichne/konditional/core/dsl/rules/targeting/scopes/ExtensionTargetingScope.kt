package io.amichne.konditional.core.dsl.rules.targeting.scopes

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.rules.predicate.NamespacePredicate
import io.amichne.konditional.rules.predicate.PredicateRef

/**
 * Targeting mix-in for custom predicates.
 */
@KonditionalDsl
interface ExtensionTargetingScope<C : Context> {
    /**
     * Adds a custom targeting extension.
     *
     * Extensions allow for domain-specific targeting beyond the standard
     * platform, locale, and version criteria. Internally, each call appends
     * a [io.amichne.konditional.rules.targeting.Targeting.Custom] leaf.
     *
     * Multiple extension blocks are accumulated and combined with AND semantics.
     * A rule matches only when all extension predicates match.
     *
     * Example:
     * ```kotlin
     * extension {
     *     organizationId == "enterprise"
     * }
     * ```
     *
     * @param block The extension logic as a lambda
     */
    fun extension(block: C.() -> Boolean)

    /**
     * Adds a named predicate reference.
     *
     * The ref is resolved exactly once during DSL construction against the namespace-scoped
     * predicate registry, then appended as a [io.amichne.konditional.rules.targeting.Targeting.Custom]
     * leaf. Unknown refs fail fast with [io.amichne.konditional.core.result.ParseError.UnknownPredicate].
     *
     * @param ref Stable predicate reference descriptor
     */
    fun predicate(ref: PredicateRef)

    /**
     * Requires a namespace-declared predicate.
     *
     * This is consumer-facing DSL sugar for [predicate], intended for predicate handles
     * declared via `Namespace.predicate { ... }`.
     */
    fun require(namedPredicate: NamespacePredicate<C>) {
        predicate(namedPredicate.ref)
    }
}

@PublishedApi
internal interface NarrowingTargetingScope<C : Context> {
    fun <R : Context> extensionNarrowed(
        evidence: (C) -> R?,
        block: R.() -> Boolean,
    )
}

/**
 * Adds a capability-narrowed extension predicate.
 *
 * The rule matches this predicate only when the runtime context is an instance
 * of [R] and [block] returns `true`. When the runtime context does not implement
 * [R], this predicate returns `false` without throwing.
 *
 * Calling this function is equivalent to adding an `extension { ... }` block,
 * so multiple calls compose with logical AND semantics and contribute to rule
 * specificity.
 *
 * @param block Predicate evaluated against a narrowed context type [R]
 */
inline fun <reified R : Context> ExtensionTargetingScope<*>.whenContext(
    crossinline block: R.() -> Boolean,
) {
    @Suppress("UNCHECKED_CAST")
    val narrowingScope = this as? NarrowingTargetingScope<Context>
    if (narrowingScope != null) {
        narrowingScope.extensionNarrowed(
            evidence = { context -> context as? R },
            block = { block() },
        )
        return
    }

    @Suppress("UNCHECKED_CAST")
    val scope = this as ExtensionTargetingScope<Context>
    scope.extension {
        val narrowed = this as? R ?: return@extension false
        narrowed.block()
    }
}
