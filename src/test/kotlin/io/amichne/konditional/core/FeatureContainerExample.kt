package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.BooleanFeature
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.IntFeature
import io.amichne.konditional.core.result.utils.evaluateOrDefault

/**
 * Example comparing current enum approach vs FeatureContainer approach
 */

// ============================================================================
// CURRENT APPROACH: Enum-based features
// ============================================================================

//enum class PaymentFeaturesEnum(override val key: String)
//    : BooleanFeature<Context, Namespace.Payments> {
//    APPLE_PAY("apple_pay"),
//    GOOGLE_PAY("google_pay"),
//    CARD_ON_FILE("card_on_file");
//
//    // ❌ Boilerplate: Must override module on every enum
//    override val module = Namespace.Payments
//}
//
// ❌ Problem 1: Can't mix types in enums
// This won't compile - enums can't have different return types per entry
// enum class MixedFeaturesEnum : Feature<???, ???, Context, Namespace.Payments>

// ❌ Problem 2: No automatic enumeration
// Have to manually maintain list of all features
//object CurrentFeatureRegistry {
//    val allPaymentFeatures = PaymentFeaturesEnum.values().toList()
//    // Must manually add each enum/object to this list
//    val allFeatures = allPaymentFeatures // + orderFeatures + accountFeatures...
//}

// ============================================================================
// NEW APPROACH: FeatureContainer with delegation
// ============================================================================

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    // ✅ Ergonomic: Clean delegation syntax
    val apple_pay by boolean<Context>(default = false)
    val google_pay by boolean<Context>(default = false)
    val card_on_file by boolean<Context>(default = false)

    // ✅ Mixed types: Can combine different feature types
    val max_cards by int<Context>(default = 5)
    val payment_provider by string<Context>(default = "stripe")

    // ✅ No boilerplate: Module declared once at container level
    // ✅ Auto-registration: All features automatically tracked
}

object OrderFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val fast_checkout by boolean<Context>(default = false)
    val order_limit by int<Context>(default = 1000)
    val discount_code by string<Context>(default = "")
}

// ============================================================================
// VALUE DEMONSTRATION
// ============================================================================

object FeatureContainerValueDemo {

    // ✅ BENEFIT 1: Complete enumeration (automatic)
    fun getAllFeatures() {
        val paymentFeatures = PaymentFeatures.allFeatures()
        val orderFeatures = OrderFeatures.allFeatures()

        println("Payment features: ${paymentFeatures.size}") // 5 features
        println("Order features: ${orderFeatures.size}")     // 3 features

        // Can iterate over all features
        paymentFeatures.forEach { feature ->
            println("Feature: ${feature.key}")
        }
    }

    // ✅ BENEFIT 2: Type safety preserved
    fun typeSafetyDemo(context: Context) {
        // Boolean feature
        val applePay: BooleanFeature<Context, Namespace.Payments> =
            PaymentFeatures.apple_pay

        // Int feature
        val maxCards: IntFeature<Context, Namespace.Payments> =
            PaymentFeatures.max_cards

        // Type inference works
        val enabled = context.evaluateOrDefault(PaymentFeatures.apple_pay, false)
        val limit = context.evaluateOrDefault(PaymentFeatures.max_cards, 5)

        // Compile-time type safety
        // val x: Int = PaymentFeatures.apple_pay // ❌ Type mismatch
    }

    // ✅ BENEFIT 3: Discovery and auditing
    fun auditFeatures() {
        // Find all features across all containers
        val allContainers = listOf(
            PaymentFeatures,
            OrderFeatures
            // Add more containers as needed
        )

        val totalFeatures = allContainers.sumOf { it.allFeatures().size }
        println("Total features in system: $totalFeatures")

        // Group by module
        allContainers.forEach { container ->
            val module = container.allFeatures().first().namespace
            println("${module.javaClass.simpleName}: ${container.allFeatures().size} features")
        }
    }

//    // ✅ BENEFIT 4: Testing - can easily get all features for comprehensive testing
//    fun <C : Context> testAllFeatures(context: C) {
//        PaymentFeatures.allFeatures().filter { it }.forEach { feature ->
//            // Type-erased, but we can still evaluate safely
//            when (val result = context.evaluate()) {
//                is EvaluationResult.Success -> println("${feature.key} = ${result.value}")
//                is EvaluationResult.FlagNotFound -> println("${feature.key} not configured")
//                is EvaluationResult.EvaluationError -> println("Error evaluating ${feature.key}: ${result.exception}")
//            }
//        }
//    }

    // ✅ BENEFIT 5: Validation - ensure all features are configured
    fun validateConfiguration() {
        val configuredKeys = setOf("apple_pay", "google_pay", "max_cards") // from config source

        val allFeatureKeys = PaymentFeatures.allFeatures().map { it.key }.toSet()
        val missing = allFeatureKeys - configuredKeys

        if (missing.isNotEmpty()) {
            println("WARNING: Features not configured: $missing")
        }
    }

    // ✅ BENEFIT 6: Documentation generation
    fun generateFeatureInventory() {
        println("# Feature Inventory\n")

        println("## Payment Features")
        PaymentFeatures.allFeatures().forEach { feature ->
            println("- `${feature.key}`: ${feature::class.simpleName}")
        }

        println("\n## Order Features")
        OrderFeatures.allFeatures().forEach { feature ->
            println("- `${feature.key}`: ${feature::class.simpleName}")
        }
    }
}

// ============================================================================
// COMPARISON SUMMARY
// ============================================================================

/*
┌─────────────────────────────┬──────────────────────┬──────────────────────────┐
│ Feature                     │ Enum Approach        │ FeatureContainer         │
├─────────────────────────────┼──────────────────────┼──────────────────────────┤
│ Declaration syntax          │ Verbose enum         │ Clean delegation (by)    │
│ Module declaration          │ Per-entry override   │ Once per container       │
│ Mixed types                 │ ❌ Not possible      │ ✅ Boolean, Int, String   │
│ Auto-enumeration            │ ❌ Manual tracking   │ ✅ allFeatures()          │
│ Boilerplate                 │ ❌ High              │ ✅ Minimal                │
│ Type safety                 │ ✅ Full              │ ✅ Full                   │
│ IDE autocomplete            │ ✅ Good              │ ✅ Good                   │
│ Refactoring support         │ ✅ Good              │ ✅ Good                   │
│ Feature discovery           │ ❌ Manual            │ ✅ Automatic              │
│ Configuration validation    │ ❌ Complex           │ ✅ Simple                 │
│ Testing all features        │ ❌ Manual collection │ ✅ allFeatures()          │
└─────────────────────────────┴──────────────────────┴──────────────────────────┘

KEY WINS:
1. 📝 Less boilerplate (no module override per entry)
2. 🎯 Complete enumeration (allFeatures() automatic)
3. 🔀 Mixed types in one container
4. 🧪 Better testing (iterate over all features)
5. ✅ Config validation (detect missing features)
6. 📊 Audit/inventory generation
*/
