@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.instance

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.schema.CompiledNamespaceSchema

/**
 * Trusted configuration payload produced only by schema-aware materialization.
 *
 * This wrapper separates boundary-parsed data from raw or ad-hoc configuration construction.
 */
@KonditionalInternalApi
@ConsistentCopyVisibility
data class MaterializedConfiguration @PublishedApi internal constructor(
    val schema: CompiledNamespaceSchema,
    val configuration: Configuration,
) {
    companion object {
        @KonditionalInternalApi
        fun of(
            schema: CompiledNamespaceSchema,
            configuration: Configuration,
        ): MaterializedConfiguration = MaterializedConfiguration(schema = schema, configuration = configuration)
    }
}
