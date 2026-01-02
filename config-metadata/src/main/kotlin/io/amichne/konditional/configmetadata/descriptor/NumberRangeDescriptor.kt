package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

data class NumberRangeDescriptor(
    override val uiHints: UiHints,
    val min: Double,
    val max: Double,
    val step: Double,
    val unit: String? = null,
) : ValueDescriptor {
    override val kind: ValueDescriptor.Kind = ValueDescriptor.Kind.NUMBER_RANGE
}
