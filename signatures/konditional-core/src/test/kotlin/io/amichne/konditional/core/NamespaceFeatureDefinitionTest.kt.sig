file=konditional-core/src/test/kotlin/io/amichne/konditional/core/NamespaceFeatureDefinitionTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.features.BooleanFeature,io.amichne.konditional.core.features.IntFeature,io.amichne.konditional.core.features.StringFeature,io.amichne.konditional.core.id.StableId,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.core.NamespaceFeatureDefinitionTest|kind=class|decl=class NamespaceFeatureDefinitionTest
type=io.amichne.konditional.core.TestFeatures|kind=object|decl=private object TestFeatures : Namespace.TestNamespaceFacade("feature-definition")
fields:
- val testBoolean by boolean<Context>(default = false)
- val testString by string<Context>(default = "default")
- val testInt by integer<Context>(default = 0)
- val testDouble by double<Context>(default = 0.0)
methods:
- fun `features have correct keys`()
- fun `features have correct module`()
- fun `allFeatures returns all declared features`()
- fun `features are eagerly initialized at container creation`()
- fun `features can be evaluated with context`()
- fun `multiple namespaces maintain independent feature lists`()
- fun `can iterate over all features for validation`()
- fun `features maintain type safety through container`()
