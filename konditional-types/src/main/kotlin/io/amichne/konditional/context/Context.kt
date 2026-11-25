package io.amichne.konditional.context

import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.core.id.StableId

/** Inputs to an evaluation. Each namespace declares the facts that its rules can inspect. */
interface Context {
    /** Immutable assignments for declared facts. An absent fact misses a positive comparison. */
    val facts: Facts
        get() = Facts.EMPTY

    /** Optional identity used by allowlists and deterministic rollout. */
    interface StableIdContext : Context {
        val stableId: StableId
    }

    companion object {
        /** Creates a context from its immutable fact assignments. */
        operator fun invoke(facts: Facts = Facts.EMPTY): Context = object : Context {
            override val facts: Facts = facts
        }
    }
}
