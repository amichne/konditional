package io.amichne.konditional.core.types

data class StringEncodeable(override val value: String) : EncodableValue<String> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.STRING
}
