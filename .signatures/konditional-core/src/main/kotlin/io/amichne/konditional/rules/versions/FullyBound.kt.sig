file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/FullyBound.kt
package=io.amichne.konditional.rules.versions
imports=io.amichne.konditional.context.Version
type=io.amichne.konditional.rules.versions.FullyBound|kind=class|decl=data class FullyBound( override val min: Version, override val max: Version, ) : VersionRange(Type.MIN_AND_MAX_BOUND, min, max)
