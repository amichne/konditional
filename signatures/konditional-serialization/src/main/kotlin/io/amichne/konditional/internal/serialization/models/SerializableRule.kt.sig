file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableRule.kt
package=io.amichne.konditional.internal.serialization.models
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.internal.SerializedFlagRuleSpec,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.internal.serialization.models.SerializableRule|kind=class|decl=data class SerializableRule( val value: FlagValue<*>, val rampUp: Double = 100.0, val rampUpAllowlist: Set<String> = emptySet(), val note: String? = null, val locales: Set<String> = emptySet(), val platforms: Set<String> = emptySet(), val versionRange: VersionRange? = null, val axes: Map<String, Set<String>> = emptyMap(), )
methods:
- fun <T : Any> toSpec(value: T): SerializedFlagRuleSpec<T>
