package io.amichne.konditional.core.types

/**
 * Bidirectional converter between domain type and primitive encoding.
 *
 * Enforces that both encoding and decoding logic are provided together,
 * preventing partial implementations and ensuring round-trip safety.
 *
 * @param I Input domain type
 * @param O Output primitive type
 */
data class Converter<I : Any, O : Any>(
    private val encodeFn: (I) -> O,
    private val decodeFn: (O) -> I,
) {
    fun encode(input: I): O = encodeFn(input)
    fun decode(output: O): I = decodeFn(output)
}
