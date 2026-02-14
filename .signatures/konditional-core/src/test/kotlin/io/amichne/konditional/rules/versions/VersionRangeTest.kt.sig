file=konditional-core/src/test/kotlin/io/amichne/konditional/rules/versions/VersionRangeTest.kt
package=io.amichne.konditional.rules.versions
imports=io.amichne.konditional.context.Version,kotlin.test.Test,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.rules.versions.VersionRangeTest|kind=class|decl=class VersionRangeTest
methods:
- fun `Unbounded hasBounds returns false`()
- fun `LeftBound hasBounds returns true`()
- fun `RightBound hasBounds returns true`()
- fun `FullyBound hasBounds returns true`()
- fun `Unbounded contains all versions`()
- fun `LeftBound contains versions at or above minimum`()
- fun `RightBound contains versions at or below maximum`()
- fun `FullyBound contains versions within range`()
