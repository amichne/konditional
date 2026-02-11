file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializablePatch.kt
package=io.amichne.konditional.internal.serialization.models
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.values.FeatureId
type=io.amichne.konditional.internal.serialization.models.SerializablePatch|kind=class|decl=data class SerializablePatch( val meta: SerializableSnapshotMetadata? = null, val flags: List<SerializableFlag>, val removeKeys: List<FeatureId> = emptyList(), )
