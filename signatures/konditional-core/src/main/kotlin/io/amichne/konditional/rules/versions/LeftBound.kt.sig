file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/LeftBound.kt
package=io.amichne.konditional.rules.versions
imports=io.amichne.konditional.context.Version
type=io.amichne.konditional.rules.versions.LeftBound|kind=class|decl=data class LeftBound( override val min: Version, ) : VersionRange(Type.MIN_BOUND, min, MAX_VERSION)
