package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Feature for custom wrapper types that encode to primitives.
 *
 * Enables extension types: "0-depth primitive-like values" such as DateTime, UUID, etc.
 *
 * @param T The wrapper type (DateTime, UUID, etc.)
 * @param P The primitive type it encodes to (String, Int, Double, Boolean)
 * @param C The context type
 * @param M The taxonomy this feature belongs to
 */
interface OfCustom<T : Any, P : Any, C : Context, M : Taxonomy> :
    Feature<EncodableValue.CustomEncodeable<T, P>, T, C, M>
