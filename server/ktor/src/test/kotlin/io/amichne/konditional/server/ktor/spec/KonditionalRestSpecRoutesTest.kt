package io.amichne.konditional.server.ktor.spec

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KonditionalRestSpecRoutesTest {
    @Test
    fun `route serves configured specification payload`() =
        testApplication {
            application {
                installKonditionalRestSpec(
                    KonditionalRestSpecRouteConfig(
                        routePath = "/spec.json",
                        specJsonProvider = { "{\"openapi\":\"3.0.3\"}" },
                    ),
                )
            }

            val response = client.get("/spec.json")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.headers["Content-Type"].orEmpty().contains("application/json"))
            assertEquals("{\"openapi\":\"3.0.3\"}", response.bodyAsText())
        }

    @Test
    fun `default route serves rest surface specification`() =
        testApplication {
            application {
                installKonditionalRestSpec()
            }

            val response = client.get("/openapi/konditional-rest-spec.json")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"openapi\""))
        }
}
