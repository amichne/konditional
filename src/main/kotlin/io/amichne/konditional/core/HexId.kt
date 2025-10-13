package io.amichne.konditional.core

@JvmInline
@OptIn(ExperimentalStdlibApi::class)
internal value class HexId internal constructor(internal val externalId: String) {
    val byteId: ByteArray
        get() = externalId.hexToByteArray(HexFormat.Default)

    val id: String
        get() = byteId.toHexString(HexFormat.Default)

    init {
        require(id == externalId)
    }
}
