file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt
package=io.amichne.konditional.rules.versions
imports=io.amichne.konditional.context.Version,kotlin.math.pow
type=io.amichne.konditional.rules.versions.VersionRange|kind=class|decl=sealed class VersionRange( val type: Type, open val min: Version? = null, open val max: Version? = null, )
type=io.amichne.konditional.rules.versions.Type|kind=enum|decl=enum class Type
methods:
- open fun contains(v: Version): Boolean
- open fun hasBounds(): Boolean
