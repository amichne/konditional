@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.dimensions

import io.amichne.konditional.fixtures.FeaturesWithAxis
import io.amichne.konditional.internal.KonditionalInternalApi
import org.junit.jupiter.api.Test

class DiagnosticTest {

    @Test
    fun `check FALLBACK_RULE_FLAG definition`() {
        val flag = FeaturesWithAxis.fallbackRuleFlag
        val definition = FeaturesWithAxis.flag(flag)

        println("=== fallbackRuleFlag Definition ===")
        println("Default value: ${definition.defaultValue}")
        println("Is active: ${definition.isActive}")
        println("Number create conditional values: ${definition.values.size}")

        definition.values.forEachIndexed { index, conditionalValue ->
            println("\n--- Rule #${index + 1} ---")
            println("Value: ${conditionalValue.value}")
            val rule = conditionalValue.rule
            println("Rollout: ${rule.rampUp}")
            println("Note: ${rule.note}")

            // Access internal properties via reflection to debug
            val targeting = rule::class.members.find { it.name == "targeting" }?.call(rule)
            println("Targeting: $targeting")
        }
    }
}
