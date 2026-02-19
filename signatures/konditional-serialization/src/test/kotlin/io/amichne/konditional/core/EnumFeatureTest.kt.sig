file=konditional-serialization/src/test/kotlin/io/amichne/konditional/core/EnumFeatureTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.features.EnumFeature,io.amichne.konditional.core.id.StableId,io.amichne.konditional.runtime.load,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.instance.MaterializedConfiguration,io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec,io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader,kotlin.test.assertTrue,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Test
type=io.amichne.konditional.core.EnumFeatureTest|kind=class|decl=class EnumFeatureTest
type=io.amichne.konditional.core.LogLevel|kind=enum|decl=enum class LogLevel
type=io.amichne.konditional.core.Theme|kind=enum|decl=enum class Theme
type=io.amichne.konditional.core.Environment|kind=enum|decl=enum class Environment
type=io.amichne.konditional.core.EnumFeatures|kind=object|decl=private object EnumFeatures : Namespace.TestNamespaceFacade("enum-features")
type=io.amichne.konditional.core.SingleValue|kind=enum|decl=enum class SingleValue
fields:
- val logLevel by enum<LogLevel, Context>(default = LogLevel.INFO)
- val theme by enum<Theme, Context>(default = Theme.AUTO)
- val environment by enum<Environment, Context>(default = Environment.PRODUCTION)
methods:
- fun `enum features have correct keys`()
- fun `enum features have correct namespace`()
- fun `enum features return default values`()
- fun `enum features evaluate with rules`()
- fun `multiple enum types can coexist in feature container`()
- fun `enum features work alongside primitive types in container`()
- fun `enum features maintain type safety through container`()
- fun `enum features can have complex rule configurations`()
- fun `enum with single value works correctly`()
- fun `enum values survive namespace snapshot roundtrip`()
