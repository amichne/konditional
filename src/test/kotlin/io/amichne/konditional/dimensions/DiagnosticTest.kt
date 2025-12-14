package io.amichne.konditional.dimensions

import io.amichne.konditional.fixtures.FeaturesWithAxis
import org.junit.jupiter.api.Test

class DiagnosticTest {

    @Test
    fun `check FALLBACK_RULE_FLAG definition`() {
        val flag = FeaturesWithAxis.FALLBACK_RULE_FLAG
        val definition = FeaturesWithAxis.namespace.flag(flag)

        println("=== FALLBACK_RULE_FLAG Definition ===")
        println("Default value: ${definition.defaultValue}")
        println("Is active: ${definition.isActive}")
        println("Number of conditional values: ${definition.values.size}")

        definition.values.forEachIndexed { index, conditionalValue ->
            println("\n--- Rule #${index + 1} ---")
            println("Value: ${conditionalValue.value}")
            val rule = conditionalValue.rule
            println("Rollout: ${rule.rollout}")
            println("Note: ${rule.note}")

            // Access internal properties via reflection to debug
            val baseEvaluable = rule::class.members.find { it.name == "baseEvaluable" }?.call(rule)
            println("BaseEvaluable: $baseEvaluable")
        }
    }
}
