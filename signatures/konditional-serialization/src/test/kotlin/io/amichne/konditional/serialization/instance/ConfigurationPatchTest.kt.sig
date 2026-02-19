file=konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatchTest.kt
package=io.amichne.konditional.serialization.instance
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.fixtures.CommonTestFeatures,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertFalse,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.instance.ConfigurationPatchTest|kind=class|decl=class ConfigurationPatchTest
methods:
- fun emptyPatchHasNoFlagsOrRemoveKeys()
- fun patchBuilderAddsFlagsCorrectly()
- fun patchBuilderRemovesFlagsCorrectly()
- fun patchBuilderHandlesAddAndRemoveForSameFeature()
- fun applyPatchAddsAndRemovesFlagsCorrectly()
