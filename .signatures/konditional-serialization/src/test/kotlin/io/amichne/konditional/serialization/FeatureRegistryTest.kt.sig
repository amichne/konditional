file=konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/FeatureRegistryTest.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseErrorOrNull,io.amichne.konditional.values.FeatureId,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue,org.junit.jupiter.api.BeforeEach,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.FeatureRegistryTest|kind=class|decl=class FeatureRegistryTest
type=io.amichne.konditional.serialization.TestFeatures|kind=object|decl=private object TestFeatures : Namespace.TestNamespaceFacade("feature-registry")
fields:
- val feature1 by boolean<Context>(default = false)
- val feature2 by string<Context>(default = "default")
- val feature3 by integer<Context>(default = 0)
methods:
- fun setup()
- fun `Given feature, When registered, Then can be retrieved by key`()
- fun `Given multiple features, When registered, Then all can be retrieved`()
- fun `Given feature, When registered twice, Then registration is idempotent`()
- fun `Given unregistered key, When retrieved, Then returns FeatureNotFound error`()
- fun `Given registered feature, When contains checked, Then returns true`()
- fun `Given unregistered feature, When contains checked, Then returns false`()
- fun `Given registered features, When cleared, Then all features are removed`()
- fun `Given empty registry, When cleared, Then no error occurs`()
- fun `Given features with different keys, When registered, Then both are stored separately`()
- fun `Given boolean feature, When retrieved, Then maintains type information`()
- fun `Given string feature, When retrieved, Then maintains type information`()
- fun `Given int feature, When retrieved, Then maintains type information`()
