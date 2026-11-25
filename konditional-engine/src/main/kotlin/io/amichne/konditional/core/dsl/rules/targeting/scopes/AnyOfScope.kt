package io.amichne.konditional.core.dsl.rules.targeting.scopes

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl

/**
 * DSL scope for [io.amichne.konditional.rules.targeting.Targeting.AnyOf] blocks.
 *
 * Exposes all targeting dimension mix-ins without rule-metadata concerns
 * ([StableIdTargetingScope], [io.amichne.konditional.core.dsl.rules.NoteScope], rampUp).
 *
 * Each call inside an `anyOf { }` block adds one alternative. A group of fact
 * comparisons produces the same portable expression as the function `anyOf(...)`.
 *
 * [whenContext] from [ExtensionTargetingScope] is available automatically as it
 * is an extension function on that interface.
 */
@KonditionalDsl
interface AnyOfScope<C : Context> :
    FactTargetingScope,
    ExtensionTargetingScope<C>
