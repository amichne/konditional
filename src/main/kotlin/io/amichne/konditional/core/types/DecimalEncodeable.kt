package io.amichne.konditional.core.types

data class DecimalEncodeable(override val value: Double) : EncodableValue<Double> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.DECIMAL
}
