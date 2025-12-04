package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EnumEncodeable

/**
 * DSL scope for enum-typed feature flags.
 * Provides type-safe DSL operations for user-defined enum types.
 *
 * @param E The specific enum type
 * @param C The context type for evaluation
 * @param M The namespace type for isolation
 */
@KonditionalDsl
interface EnumScope<E : Enum<E>, C : Context, M : Namespace> :
    FlagScope<EnumEncodeable<E>, E, C, M>
