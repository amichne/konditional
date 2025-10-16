package io.amichne.konditional.core

@JvmInline
@OptIn(ExperimentalStdlibApi::class)
/**
 * Represents a hexadecimal identifier.
 *
 * @property externalId The underlying string value of the hexadecimal identifier.
 * @constructor Internal constructor to restrict instantiation.
 */
value class HexId internal constructor(internal val externalId: String) {
    private val byteId: ByteArray
        get() = externalId.hexToByteArray(HexFormat.Default)

    /**
     * Unique identifier represented as a hexadecimal string.
     *
     * Throws an [IllegalArgumentException] if the `externalId` is not a valid hexadecimal string.
     */
    val id: String
        get() = byteId.toHexString(HexFormat.Default)

    init {
        require(id == externalId)
    }
}
