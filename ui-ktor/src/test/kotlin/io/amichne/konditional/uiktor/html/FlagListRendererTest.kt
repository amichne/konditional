@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.html

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.values.FeatureId
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagListRendererTest {
    @Test
    fun `groupFlagsByNamespace sorts by namespace`() {
        val snapshot =
            SerializableSnapshot(
                flags =
                    listOf(
                        SerializableFlag(
                            key = FeatureId.create("ui", "dark_mode"),
                            defaultValue = FlagValue.BooleanValue(false),
                        ),
                        SerializableFlag(
                            key = FeatureId.create("payments", "provider"),
                            defaultValue = FlagValue.StringValue("stripe"),
                        ),
                    ),
            )

        val namespaces = groupFlagsByNamespace(snapshot).map(FlagsByNamespace::namespace)

        assertEquals(listOf("payments", "ui"), namespaces)
    }

    @Test
    fun `renderFlagListPage includes key and inactive badge`() {
        val featureId = FeatureId.create("ui", "dark_mode")
        val snapshot =
            SerializableSnapshot(
                flags =
                    listOf(
                        SerializableFlag(
                            key = featureId,
                            defaultValue = FlagValue.BooleanValue(false),
                            isActive = false,
                        ),
                    ),
            )

        val html = createHTML().div { renderFlagListPage(snapshot, "/config") }

        assertTrue(html.contains("Feature Flags"))
        assertTrue(html.contains("dark_mode"))
        assertTrue(html.contains("Inactive"))
        assertTrue(html.contains("hx-get=\"/config/flag/${featureId}\""))
    }
}
