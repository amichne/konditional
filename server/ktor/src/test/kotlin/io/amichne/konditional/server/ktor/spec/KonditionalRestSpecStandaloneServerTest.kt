package io.amichne.konditional.server.ktor.spec

import kotlin.test.Test
import kotlin.test.assertEquals

class KonditionalRestSpecStandaloneServerTest {
    @Test
    fun `parse server args uses defaults when no args provided`() {
        val config = parseServerArgs(emptyArray())

        assertEquals("0.0.0.0", config.host)
        assertEquals(8080, config.port)
        assertEquals("/openapi/konditional-rest-spec.json", config.routeConfig.routePath)
    }

    @Test
    fun `parse server args applies provided host port and path`() {
        val config =
            parseServerArgs(
                arrayOf(
                    "--host=127.0.0.1",
                    "--port=9191",
                    "--path=/spec/openapi.json",
                ),
            )

        assertEquals("127.0.0.1", config.host)
        assertEquals(9191, config.port)
        assertEquals("/spec/openapi.json", config.routeConfig.routePath)
    }
}
