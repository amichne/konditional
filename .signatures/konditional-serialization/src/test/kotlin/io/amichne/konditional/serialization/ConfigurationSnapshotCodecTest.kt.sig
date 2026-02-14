file=konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/ConfigurationSnapshotCodecTest.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.axisValues,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.fixtures.serializers.RetryPolicy,io.amichne.konditional.fixtures.utilities.update,io.amichne.konditional.runtime.load,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.options.SnapshotLoadOptions,io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec,io.amichne.konditional.values.FeatureId,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertIs,kotlin.test.assertNotNull,kotlin.test.assertTrue,org.junit.jupiter.api.BeforeEach,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.ConfigurationSnapshotCodecTest|kind=class|decl=class ConfigurationSnapshotCodecTest
type=io.amichne.konditional.serialization.TestFeatures|kind=object|decl=private object TestFeatures : Namespace.TestNamespaceFacade("snapshot-serializer")
type=io.amichne.konditional.serialization.Environment|kind=enum|decl=private enum class Environment(override val id: String) : AxisValue<Environment>
type=io.amichne.konditional.serialization.Tenant|kind=enum|decl=private enum class Tenant(override val id: String) : AxisValue<Tenant>
type=io.amichne.konditional.serialization.Axes|kind=object|decl=private object Axes
type=io.amichne.konditional.serialization.Theme|kind=enum|decl=private enum class Theme
fields:
- val boolFlag by boolean<Context>(default = false)
- val stringFlag by string<Context>(default = "default")
- val intFlag by integer<Context>(default = 0)
- val doubleFlag by double<Context>(default = 0.0)
- val themeFlag by enum<Theme, Context>(default = Theme.LIGHT)
- val retryPolicyFlag by custom<RetryPolicy, Context>(default = RetryPolicy())
- val EnvironmentAxis
- val TenantAxis
methods:
- fun setup()
- private fun featureIndexById()
- private fun decodeFeatureAware( json: String, options: SnapshotLoadOptions = SnapshotLoadOptions.strict(), ): ParseResult<Configuration>
- fun `Given feature-aware decode context, When decoded, Then decode succeeds`()
- fun `Given snapshot with flags, When decoded without feature scope by default, Then returns typed failure`()
- fun `Given snapshot with flags, When decoded without feature scope and skipUnknownKeys option, Then returns typed failure`()
- fun `Given forged enum class name in payload, When decoding feature-aware, Then trusted feature type is used`()
- fun `Given forged data class name in payload, When decoding feature-aware, Then trusted feature type is used`()
- private fun ctx( idHex: String, locale: AppLocale = AppLocale.UNITED_STATES, platform: Platform = Platform.IOS, version: String = "1.0.0", )
- private fun ctxWithEnvironment(env: Environment): Context
- fun `Given empty Konfig, When serialized, Then produces valid JSON with empty flags array`()
- fun `Given Konfig with boolean flag, When serialized, Then includes flag with correct type`()
- fun `Given Konfig with string flag, When serialized, Then includes flag with correct type`()
- fun `Given Konfig with int flag, When serialized, Then includes flag with correct type`()
- fun `Given Konfig with double flag, When serialized, Then includes flag with correct type`()
- fun `Given Konfig with complex rules, When serialized, Then includes all rule attributes`()
- fun `Given Konfig with rollout allowlists, When round-tripped, Then allowlists are preserved`()
- fun `Given Konfig with axis targeting, When serialized and round-tripped, Then axes constraints are preserved`()
- fun `Given maximal snapshot, When serialized, Then output includes all supported fields`()
- fun `Given Konfig with multiple flags, When serialized, Then includes all flags`()
- fun `Given valid JSON with empty flags, When deserialized, Then returns success with empty Konfig`()
- fun `Given valid JSON with boolean flag, When deserialized, Then returns success with correct flag`()
- fun `Given valid JSON with string flag, When deserialized, Then returns success with correct flag`()
- fun `Given valid JSON with int flag, When deserialized, Then returns success with correct flag`()
- fun `Given valid JSON with double flag, When deserialized, Then returns success with correct flag`()
- fun `Given JSON with complex rule, When deserialized, Then returns success with all rule attributes`()
- fun `Given invalid JSON, When deserialized, Then returns failure with InvalidJson error`()
- fun `Given JSON with unregistered feature, When deserialized, Then returns failure with FeatureNotFound error`()
- fun `Given boolean flag, When round-tripped, Then deserialized value equals original`()
- fun `Given string flag, When round-tripped, Then deserialized value equals original`()
- fun `Given int flag, When round-tripped, Then deserialized value equals original`()
- fun `Given double flag, When round-tripped, Then deserialized value equals original`()
- fun `Given flag with complex rules, When round-tripped, Then all rule attributes are preserved`()
- fun `Given multiple flags, When round-tripped, Then all flags are preserved`()
- fun `Given patch with new flag, When applied, Then new flag is added to konfig`()
- fun `Given patch with updated flag, When applied, Then flag is updated in konfig`()
- fun `Given patch with remove key, When applied, Then flag is removed from konfig`()
- fun `Given patch with multiple operations, When applied, Then all operations are executed`()
- fun `Given invalid patch JSON, When applied, Then returns failure`()
- fun `Given direct patch application, When valid, Then applies patch correctly`()
