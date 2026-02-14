file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/RightBound.kt
package=io.amichne.konditional.rules.versions
imports=io.amichne.konditional.context.Version
type=io.amichne.konditional.rules.versions.RightBound|kind=class|decl=data class RightBound( override val max: Version, ) : VersionRange(Type.MAX_BOUND, MIN_VERSION, max)
