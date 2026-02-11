file=konditional-core/src/main/kotlin/io/amichne/konditional/internal/builders/versions/VersionRangeBuilder.kt
package=io.amichne.konditional.internal.builders.versions
imports=io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.VersionRangeScope,io.amichne.konditional.rules.versions.FullyBound,io.amichne.konditional.rules.versions.LeftBound,io.amichne.konditional.rules.versions.RightBound,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.internal.builders.versions.VersionRangeBuilder|kind=class|decl=internal data class VersionRangeBuilder( private var leftBound: Version = Version.default, private var rightBound: Version = Version.default, ) : VersionRangeScope
methods:
- override fun min( major: Int, minor: Int, patch: Int, )
- override fun max( major: Int, minor: Int, patch: Int, )
- fun build(): VersionRange
