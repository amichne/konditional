file=konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedIntegrationTest.kt
package=io.amichne.konditional.serialization
imports=com.squareup.moshi.Moshi,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.core.result.utils.onSuccess,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.fixtures.core.withOverride,io.amichne.konditional.fixtures.serializers.UserSettings,io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec,io.amichne.kontracts.dsl.jsonObject,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertNotNull,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.serialization.KonstrainedIntegrationTest|kind=class|decl=class KonstrainedIntegrationTest
methods:
- fun `data class to JsonValue conversion is correct`()
- fun `JsonValue to data class parsing is correct`()
- fun `schema generation and validation works as expected`()
- fun `Moshi serialization and deserialization roundtrip works`()
- fun `feature flag integration with data class works`()
