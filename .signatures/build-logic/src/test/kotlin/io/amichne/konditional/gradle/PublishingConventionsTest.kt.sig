file=build-logic/src/test/kotlin/io/amichne/konditional/gradle/PublishingConventionsTest.kt
package=io.amichne.konditional.gradle
imports=kotlin.test.Test,kotlin.test.assertEquals
type=io.amichne.konditional.gradle.PublishingConventionsTest|kind=class|decl=class PublishingConventionsTest
methods:
- fun `deriveModuleName converts kebab-case artifact ids to title words`()
- fun `deriveModuleName normalizes underscores and dots`()
- fun `deriveModuleDescription prefers non-blank project description`()
- fun `deriveModuleDescription falls back to module name when description is blank`()
