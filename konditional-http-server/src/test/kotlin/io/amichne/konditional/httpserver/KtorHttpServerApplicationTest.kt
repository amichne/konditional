package io.amichne.konditional.httpserver

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorHttpServerApplicationTest {
    @Test
    fun `health endpoint returns ok for any method`() = withStorage { storage ->
        testApplication {
            application { konditionalHttpServer(storage) }

            val getResponse = client.get("/health")
            assertEquals(HttpStatusCode.OK, getResponse.status)
            assertEquals("ok", getResponse.bodyAsText())

            val postResponse = client.post("/health")
            assertEquals(HttpStatusCode.OK, postResponse.status)
            assertEquals("ok", postResponse.bodyAsText())
        }
    }

    @Test
    fun `namespace list endpoint returns deterministic key ordering`() = withStorage { storage ->
        storage.put("zeta", "{}")
        storage.put("alpha", "{}")

        testApplication {
            application { konditionalHttpServer(storage) }

            val response = client.get("/v1/namespaces")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[\"alpha\",\"zeta\"]", response.bodyAsText())
        }
    }

    @Test
    fun `non-GET on namespace list endpoint returns not found`() = withStorage { storage ->
        testApplication {
            application { konditionalHttpServer(storage) }

            val response = client.post("/v1/namespaces")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("not found", response.bodyAsText())
        }
    }

    @Test
    fun `namespace endpoint supports put get and delete`() = withStorage { storage ->
        testApplication {
            application { konditionalHttpServer(storage) }

            val putResponse = client.put("/v1/namespaces/app") {
                setBody("{\"flags\":{\"checkout\":true}}")
            }
            assertEquals(HttpStatusCode.NoContent, putResponse.status)

            val getResponse = client.get("/v1/namespaces/app")
            assertEquals(HttpStatusCode.OK, getResponse.status)
            assertEquals("{\"flags\":{\"checkout\":true}}", getResponse.bodyAsText())

            val deleteResponse = client.delete("/v1/namespaces/app")
            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            val missingResponse = client.get("/v1/namespaces/app")
            assertEquals(HttpStatusCode.NotFound, missingResponse.status)
            assertEquals("namespace not found", missingResponse.bodyAsText())
        }
    }

    @Test
    fun `namespace endpoint validates blank namespace and body and method`() = withStorage { storage ->
        testApplication {
            application { konditionalHttpServer(storage) }

            val blankNamespaceResponse = client.get("/v1/namespaces/")
            assertEquals(HttpStatusCode.BadRequest, blankNamespaceResponse.status)
            assertEquals("namespace must not be blank", blankNamespaceResponse.bodyAsText())

            val blankBodyResponse = client.put("/v1/namespaces/app") { setBody(" ") }
            assertEquals(HttpStatusCode.BadRequest, blankBodyResponse.status)
            assertEquals("request body must not be blank", blankBodyResponse.bodyAsText())

            val methodResponse = client.request("/v1/namespaces/app") {
                method = HttpMethod.Patch
            }
            assertEquals(HttpStatusCode.MethodNotAllowed, methodResponse.status)
            assertEquals("method not allowed", methodResponse.bodyAsText())
        }
    }

    private fun withStorage(test: (FileBackedSnapshotStorage) -> Unit) {
        val directory = Files.createTempDirectory("konditional-http-ktor")
        try {
            test(FileBackedSnapshotStorage(directory.resolve("snapshots.json")))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
