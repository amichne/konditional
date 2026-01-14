@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.html

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableRule
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleEditorRendererTest {
    @Test
    fun `renderRuleEditor shows note, ramp, and delete actions`() {
        val rule =
            SerializableRule(
                value = FlagValue.BooleanValue(true),
                rampUp = 50.0,
                note = "Beta users",
                locales = setOf("UNITED_STATES"),
                platforms = setOf("IOS"),
                axes = mapOf("tier" to setOf("beta")),
            )

        val html =
            createHTML().div {
                renderRuleEditor(
                    rule = rule,
                    ruleIndex = 0,
                    flagKey = "feature::ui::dark_mode",
                    basePath = "/config",
                )
            }

        assertTrue(html.contains("Description"))
        assertTrue(html.contains("hx-post=\"/config/flag/feature::ui::dark_mode/rule/0/note\""))
        assertTrue(html.contains("type=\"range\""))
        assertTrue(html.contains("50%"))
        assertTrue(html.contains("Delete Rule"))
        assertTrue(html.contains("IOS"))
        assertTrue(html.contains("UNITED_STATES"))
        assertTrue(html.contains("tier"))
        assertTrue(html.contains("beta"))
    }
}
