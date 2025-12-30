package io.amichne.konditional.configstate

data class NumberRangeDescriptor(
    override val uiHints: UiHints,
    val min: Double,
    val max: Double,
    val step: Double,
    val unit: String? = null,
) : FieldDescriptor {
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.NUMBER_RANGE
}
