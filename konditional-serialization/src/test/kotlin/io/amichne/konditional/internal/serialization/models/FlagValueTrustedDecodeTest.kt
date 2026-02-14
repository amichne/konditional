@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlagValueTrustedDecodeTest {
    private enum class Theme {
        LIGHT,
        DARK,
    }

    @Test
    fun `enum decode uses trusted metadata and ignores payload class hint`() {
        val decoded =
            FlagValue.EnumValue(
                value = Theme.DARK.name,
                enumClassName = "evil.payload.FakeEnum",
            ).extractValue<Theme>(expectedSample = Theme.LIGHT)

        assertEquals(Theme.DARK, decoded)
    }

    @Test
    fun `enum decode without trusted metadata fails`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                FlagValue.EnumValue(
                    value = Theme.DARK.name,
                    enumClassName = Theme::class.java.name,
                ).extractValue<Theme>()
            }

        assertTrue(error.message.orEmpty().contains("Missing trusted enum metadata"))
    }

    @Test
    fun `data class decode uses trusted metadata and ignores payload class hint`() {
        val expected = RetryPolicy(maxAttempts = 9, backoffMs = 2500.0, enabled = false, mode = "linear")
        val payload = (FlagValue.from(expected) as FlagValue.DataClassValue)
            .copy(dataClassName = "evil.payload.FakePolicy")

        val decoded = payload.extractValue<RetryPolicy>(expectedSample = RetryPolicy())

        assertEquals(expected, decoded)
    }

    @Test
    fun `data class decode without trusted metadata fails`() {
        val payload = FlagValue.from(RetryPolicy()) as FlagValue.DataClassValue

        val error =
            assertFailsWith<IllegalArgumentException> {
                payload.extractValue<RetryPolicy>()
            }

        assertTrue(error.message.orEmpty().contains("Missing trusted data-class metadata"))
    }
}
