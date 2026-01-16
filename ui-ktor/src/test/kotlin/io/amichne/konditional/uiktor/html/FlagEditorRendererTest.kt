@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.html

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.values.FeatureId
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagEditorRendererTest {
    @Test
    fun `renderFlagEditor shows header and empty rules state`() {
        val flag =
            SerializableFlag(
                key = FeatureId.create("ui", "dark_mode"),
                defaultValue = FlagValue.BooleanValue(true),
                isActive = true,
                rules = emptyList(),
            )

        val html = createHTML().div { renderFlagEditor(flag, "/config") }

        assertTrue(html.contains("dark_mode"))
        assertTrue(html.contains("Targeting Rules"))
        assertTrue(html.contains("No rules defined"))
        assertTrue(html.contains("hx-get=\"/config\""))
    }

    @Test
    fun `renderValueEditor boolean uses checkbox input`() {
        val html =
            createHTML().div {
                renderValueEditor(FlagValue.BooleanValue(true), "flag/ui/default", "/config")
            }

        assertTrue(html.contains("type=\"checkbox\""))
        assertTrue(html.contains("data-state=\"checked\""))
    }
}
