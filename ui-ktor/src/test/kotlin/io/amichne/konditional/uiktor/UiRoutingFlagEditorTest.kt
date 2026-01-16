@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.values.FeatureId
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiRoutingFlagEditorTest {
    @Test
    fun `installFlagEditorRoute serves editor html`() =
        testApplication {
            val snapshot = snapshotForTest()
            application {
                routing {
                    installFlagEditorRoute(snapshot)
                }
            }

            val response = client.get("/config/flag/${snapshot.flags.first().key}")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Targeting Rules"))
        }

    private fun snapshotForTest(): SerializableSnapshot =
        SerializableSnapshot(
            flags =
                listOf(
                    SerializableFlag(
                        key = FeatureId.create("ui", "dark_mode"),
                        defaultValue = FlagValue.BooleanValue(true),
                    ),
                ),
        )
}
