package io.amichne.konditional.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NamespaceDefaultsTest {
    private object DefaultNamespace : Namespace()

    @Test
    fun `namespace id defaults to fully qualified class name`() {
        assertEquals(DefaultNamespace::class.java.name, DefaultNamespace.id)
    }
}
