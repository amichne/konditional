package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.json.JsonObjectEncodeable
import io.amichne.konditional.core.types.json.JsonValue

/**
 * DSL scope for JSON object-typed feature flags.
 * Provides type-safe DSL operations for JSON object features.
 *
 * @param C The context type for evaluation
 * @param M The namespace type for isolation
 */
@KonditionalDsl
interface JsonObjectScope<C : Context, M : Namespace> :
    FlagScope<JsonObjectEncodeable, JsonValue.JsonObject, C, M>
