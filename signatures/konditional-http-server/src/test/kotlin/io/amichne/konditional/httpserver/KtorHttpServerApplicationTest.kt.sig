file=konditional-http-server/src/test/kotlin/io/amichne/konditional/httpserver/KtorHttpServerApplicationTest.kt
package=io.amichne.konditional.httpserver
imports=io.ktor.client.request.delete,io.ktor.client.request.get,io.ktor.client.request.post,io.ktor.client.request.put,io.ktor.client.request.request,io.ktor.client.request.setBody,io.ktor.client.statement.bodyAsText,io.ktor.http.HttpMethod,io.ktor.http.HttpStatusCode,io.ktor.server.testing.testApplication,java.nio.file.Files,kotlin.test.Test,kotlin.test.assertEquals
type=io.amichne.konditional.httpserver.KtorHttpServerApplicationTest|kind=class|decl=class KtorHttpServerApplicationTest
methods:
- fun `health endpoint returns ok for any method`()
- fun `namespace list endpoint returns deterministic key ordering`()
- fun `non-GET on namespace list endpoint returns not found`()
- fun `namespace endpoint supports put get and delete`()
- fun `namespace endpoint validates blank namespace and body and method`()
- private fun withStorage(test: (FileBackedSnapshotStorage) -> Unit)
