package io.amichne.konditional.server.ktor

import io.amichne.konditional.server.core.SurfaceRestSpecification
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KonditionalSpecRouteTest {
    @Test
    fun `installs spec route into application and returns deterministic json`() =
        testApplication {
            application {
                installKonditionalSpecRoute()
            }

            val response = client.get("/konditional/spec/openapi.json")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(SurfaceRestSpecification.openApiJson(), response.bodyAsText())
        }

    @Test
    fun `allows mounting route on a custom path`() =
        testApplication {
            application {
                installKonditionalSpecRoute(
                    KonditionalSpecRouteConfig(path = "/custom/spec.json"),
                )
            }

            val response = client.get("/custom/spec.json")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"openapi\": \"3.0.3\""))
        }
}
