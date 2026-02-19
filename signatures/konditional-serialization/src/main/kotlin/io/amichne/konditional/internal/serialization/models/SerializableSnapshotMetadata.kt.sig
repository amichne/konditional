file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshotMetadata.kt
package=io.amichne.konditional.internal.serialization.models
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.serialization.instance.ConfigurationMetadata
type=io.amichne.konditional.internal.serialization.models.SerializableSnapshotMetadata|kind=class|decl=data class SerializableSnapshotMetadata( val version: String? = null, val generatedAtEpochMillis: Long? = null, val source: String? = null, )
methods:
- fun toConfigurationMetadata(): ConfigurationMetadata
