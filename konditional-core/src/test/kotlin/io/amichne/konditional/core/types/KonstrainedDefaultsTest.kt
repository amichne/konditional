package io.amichne.konditional.core.types

import io.amichne.kontracts.schema.ObjectSchema
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KonstrainedDefaultsTest {
    private data class DefaultSchemaConfig(
        val enabled: Boolean,
    ) : Konstrained.Object<ObjectSchema>

    @Test
    fun `konstrained object defaults to empty object schema`() {
        assertTrue(DefaultSchemaConfig(enabled = true).schema.fields.isEmpty())
    }
}
