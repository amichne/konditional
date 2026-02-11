file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt
package=io.amichne.konditional.rules.versions
imports=com.squareup.moshi.ToJson,io.amichne.konditional.context.Version
type=io.amichne.konditional.rules.versions.Unbounded|kind=object|decl=data object Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION)
methods:
- override fun contains(v: Version): Boolean
- override fun hasBounds(): Boolean
