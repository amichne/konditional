package io.amichne.konditional.context

import io.amichne.konditional.core.id.StableId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LeafTypesTest {
    @Test
    fun `stable ids are deterministic and lowercase normalized`() {
        val first = StableId.of("User-1")
        val second = StableId.of("user-1")

        assertEquals(first.hexId.id, second.hexId.id)
        assertFailsWith<IllegalArgumentException> { StableId.of("  ") }
    }
}
