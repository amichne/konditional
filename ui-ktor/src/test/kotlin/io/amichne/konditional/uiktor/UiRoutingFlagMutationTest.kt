@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.uiktor.state.InMemoryFlagStateService
import io.amichne.konditional.values.FeatureId
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiRoutingFlagMutationTest {
    @Test
    fun `toggle route flips active state`() =
        testApplication {
            val snapshot = snapshotWithRules()
            val service = InMemoryFlagStateService(snapshot)
            application {
                routing {
                    installFlagMutationRoutes(service)
                }
            }

            val response = client.post("/config/flag/${snapshot.flags.first().key}/toggle")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("data-state=\"unchecked\""))
        }

    @Test
    fun `add rule route appends rule card`() =
        testApplication {
            val snapshot = snapshotWithoutRules()
            val service = InMemoryFlagStateService(snapshot)
            application {
                routing {
                    installFlagMutationRoutes(service)
                }
            }

            val response = client.post("/config/flag/${snapshot.flags.first().key}/rule")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Rule 1"))
        }

    @Test
    fun `ramp route updates rule value`() =
        testApplication {
            val snapshot = snapshotWithRules()
            val service = InMemoryFlagStateService(snapshot)
            application {
                routing {
                    installFlagMutationRoutes(service)
                }
            }

            val response =
                client.post("/config/flag/${snapshot.flags.first().key}/rule/0/ramp") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("ramp=25")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(25.0, service.getSnapshot().flags.first().rules.first().rampUp)
        }

    @Test
    fun `note route updates rule note`() =
        testApplication {
            val snapshot = snapshotWithRules()
            val service = InMemoryFlagStateService(snapshot)
            application {
                routing {
                    installFlagMutationRoutes(service)
                }
            }

            val response =
                client.post("/config/flag/${snapshot.flags.first().key}/rule/0/note") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("note=Updated")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Updated", service.getSnapshot().flags.first().rules.first().note)
        }

    @Test
    fun `delete route removes rule`() =
        testApplication {
            val snapshot = snapshotWithRules()
            val service = InMemoryFlagStateService(snapshot)
            application {
                routing {
                    installFlagMutationRoutes(service)
                }
            }

            val response = client.delete("/config/flag/${snapshot.flags.first().key}/rule/0")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(0, service.getSnapshot().flags.first().rules.size)
        }

    private fun snapshotWithoutRules(): SerializableSnapshot =
        SerializableSnapshot(
            flags =
                listOf(
                    SerializableFlag(
                        key = FeatureId.create("ui", "dark_mode"),
                        defaultValue = FlagValue.BooleanValue(false),
                    ),
                ),
        )

    private fun snapshotWithRules(): SerializableSnapshot =
        SerializableSnapshot(
            flags =
                listOf(
                    SerializableFlag(
                        key = FeatureId.create("ui", "dark_mode"),
                        defaultValue = FlagValue.BooleanValue(false),
                        isActive = true,
                        rules =
                            listOf(
                                SerializableRule(
                                    value = FlagValue.BooleanValue(true),
                                    rampUp = 100.0,
                                    note = "Rule",
                                ),
                            ),
                    ),
                ),
        )
}
