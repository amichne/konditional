package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.json.JsonArrayEncodeable
import io.amichne.konditional.core.types.json.JsonValue

/**
 * DSL scope for JSON array-typed feature flags.
 * Provides type-safe DSL operations for JSON array features.
 *
 * @param C The context type for evaluation
 * @param M The namespace type for isolation
 */
@KonditionalDsl
interface JsonArrayScope<C : Context, M : Namespace> :
    FlagScope<JsonArrayEncodeable, JsonValue.JsonArray, C, M>
