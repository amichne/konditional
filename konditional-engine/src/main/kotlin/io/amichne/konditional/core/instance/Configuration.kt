@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.core.instance

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import java.util.Collections

/** Owns an insertion-ordered, unmodifiable snapshot of the supplied flag entries. */
class Configuration(
    flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>,
    val metadata: ConfigurationMetadata = ConfigurationMetadata(),
) {
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>> = Collections.unmodifiableMap(LinkedHashMap(flags))

    fun copy(
        flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>> = this.flags,
        metadata: ConfigurationMetadata = this.metadata,
    ): Configuration = Configuration(flags, metadata)

    operator fun component1(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>> = flags
    operator fun component2(): ConfigurationMetadata = metadata

    override fun equals(other: Any?): Boolean =
        this === other || (other is Configuration && flags == other.flags && metadata == other.metadata)

    override fun hashCode(): Int = 31 * flags.hashCode() + metadata.hashCode()

    override fun toString(): String = "Configuration(flags=$flags, metadata=$metadata)"
}

data class ConfigurationMetadata(
    val version: String? = null,
    val generatedAtEpochMillis: Long? = null,
    val source: String? = null,
)

// A heterogeneous flag table erases the type shared by each feature and its definition.
@Suppress("UNCHECKED_CAST")
internal fun <T : Any, C : Context, M : Namespace<*>>
    Map<Feature<*, *, *>, FlagDefinition<*, *, *>>.definitionFor(feature: Feature<T, C, M>): FlagDefinition<T, C, M>? =
    this[feature] as FlagDefinition<T, C, M>?
