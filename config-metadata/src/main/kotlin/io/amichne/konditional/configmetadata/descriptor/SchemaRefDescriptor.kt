package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

data class SchemaRefDescriptor(
    override val uiHints: UiHints,
    val ref: String,
) : ValueDescriptor {
    override val kind: ValueDescriptor.Kind = ValueDescriptor.Kind.SCHEMA_REF
}
