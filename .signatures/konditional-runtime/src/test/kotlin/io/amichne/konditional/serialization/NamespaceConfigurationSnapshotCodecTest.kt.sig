file=konditional-runtime/src/test/kotlin/io/amichne/konditional/serialization/NamespaceConfigurationSnapshotCodecTest.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.disable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseErrorOrNull,io.amichne.konditional.fixtures.utilities.update,io.amichne.konditional.runtime.load,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.instance.MaterializedConfiguration,io.amichne.konditional.serialization.options.SnapshotLoadOptions,io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec,io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader,io.amichne.konditional.values.FeatureId,kotlin.test.assertEquals,kotlin.test.assertIs,kotlin.test.assertNotNull,kotlin.test.assertTrue,org.junit.jupiter.api.BeforeEach,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.NamespaceConfigurationSnapshotCodecTest|kind=class|decl=class NamespaceConfigurationSnapshotCodecTest
fields:
- private val testNamespace
methods:
- fun setup()
- private fun materialize(configuration: Configuration): MaterializedConfiguration
- private fun declaredDefaultConfiguration(): Configuration
- private fun ctx( idHex: String, locale: AppLocale = AppLocale.UNITED_STATES, platform: Platform = Platform.IOS, version: String = "1.0.0", )
- fun `Given namespace with no flags, When serialized, Then produces JSON with empty flags array`()
- fun `Given namespace with configured flags, When serialized, Then includes all flags`()
- fun `Given valid JSON, When deserialized, Then loads into namespace and returns success`()
- fun `Given invalid JSON, When deserialized, Then returns failure without loading`()
- fun `Given JSON with unregistered feature, When deserialized, Then returns failure`()
- fun `Given namespace, When round-tripped, Then configuration is preserved`()
- fun `Given forModule factory, When created, Then works same as constructor`()
- fun `Given different containers, When serialized separately, Then each has only its own flags`()
- fun `namespace loader deserializes without relying on global feature registry`()
- fun `namespace loader rejects snapshot from different namespace`()
