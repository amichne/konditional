package io.amichne.konditional.core

@JvmInline
@OptIn(ExperimentalStdlibApi::class)
value class HexId internal constructor(internal val externalId: String) {
    internal val byteId: ByteArray
        get() = externalId.hexToByteArray(HexFormat.Default)

    val id: String
        get() = byteId.toHexString(HexFormat.Default)

    init {
        require(id == externalId)
    }
}
