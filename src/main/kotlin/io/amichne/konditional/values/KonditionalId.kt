package io.amichne.konditional.values

/**
 * Canonically-encoded, strongly-typed identifiers.
 *
 * Identifier values are intended to be:
 * - Primary keys at storage boundaries (serialization, registries, diffs)
 * - Deterministic and comparable
 * - Resistant to accidental mixing across domains via distinct types
 *
 * Encoding convention:
 * `"<prefix>::<component1>::<component2>::..."`
 *
 * `prefix` is an explicit domain discriminator (e.g. `feature`), and components must be non-blank
 * and must not contain [IdentifierEncoding.SEPARATOR].
 */
interface KonditionalId<T : KonditionalId<T>> : Comparable<T> {
    val plainId: String

    val prefix: String
}
