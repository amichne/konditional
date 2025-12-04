package io.amichne.konditional.core.types

data class BooleanEncodeable(override val value: Boolean) : EncodableValue<Boolean> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.BOOLEAN
}
