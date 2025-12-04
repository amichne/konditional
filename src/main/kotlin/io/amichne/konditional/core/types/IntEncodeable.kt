package io.amichne.konditional.core.types

data class IntEncodeable(override val value: Int) : EncodableValue<Int> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.INTEGER
}
