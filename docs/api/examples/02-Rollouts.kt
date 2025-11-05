package examples

import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.id.StableId

/**
 * Example 1: Gradual Percentage Rollout
 *
 * Roll out a feature to a percentage of users deterministically.
 */
fun gradualRollout() {
    val BETA_FEATURE: Conditional<Boolean, Context> =
        Conditional("beta_feature")

    ConfigBuilder.config {
        BETA_FEATURE with {
            default(value = false)

            // Roll out to 25% of users
            rule {
                rollout = Rollout.of(25.0)
            } implies true
        }
    }

    // Simulate 100 users
    val results = (1..100).map { userId ->
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("user-$userId")
        )
        context.evaluate(BETA_FEATURE)
    }

    val enabledCount = results.count { it }
    println("Enabled for $enabledCount out of 100 users (~25%)")
}

/**
 * Example 2: Phased Rollout
 *
 * Multiple rollout phases with increasing percentages.
 */
fun phasedRollout() {
    val NEW_FEATURE: Conditional<String, Context> =
        Conditional("new_feature")

    // Phase 1: 10% rollout
    ConfigBuilder.config {
        NEW_FEATURE with {
            default(value = "control")

            rule {
                note("Phase 1: Internal testing")
                rollout = Rollout.of(10.0)
            } implies "phase-1"
        }
    }

    println("=== Phase 1: 10% Rollout ===")
    testRollout(NEW_FEATURE)

    // Later: Phase 2: 50% rollout
    ConfigBuilder.config {
        NEW_FEATURE with {
            default(value = "control")

            rule {
                note("Phase 2: Expanded testing")
                rollout = Rollout.of(50.0)
            } implies "phase-2"
        }
    }

    println("\n=== Phase 2: 50% Rollout ===")
    testRollout(NEW_FEATURE)

    // Finally: Phase 3: 100% rollout
    ConfigBuilder.config {
        NEW_FEATURE with {
            default(value = "control")

            rule {
                note("Phase 3: General availability")
                rollout = Rollout.of(100.0)
            } implies "phase-3"
        }
    }

    println("\n=== Phase 3: 100% Rollout ===")
    testRollout(NEW_FEATURE)
}

private fun testRollout(flag: Conditional<String, Context>) {
    val results = (1..10).map { userId ->
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("user-$userId")
        )
        context.evaluate(flag)
    }

    val distribution = results.groupingBy { it }.eachCount()
    distribution.forEach { (variant, count) ->
        println("$variant: $count/10 users")
    }
}

/**
 * Example 3: Platform-Specific Rollout
 *
 * Different rollout percentages for different platforms.
 */
fun platformSpecificRollout() {
    val EXPERIMENTAL_FEATURE: Conditional<Boolean, Context> =
        Conditional("experimental")

    ConfigBuilder.config {
        EXPERIMENTAL_FEATURE with {
            default(value = false)

            // 100% rollout on iOS (already tested)
            rule {
                platforms(Platform.IOS)
            } implies true

            // 50% rollout on Android (testing)
            rule {
                platforms(Platform.ANDROID)
                rollout = Rollout.of(50.0)
            } implies true

            // 10% rollout on Web (early testing)
            rule {
                platforms(Platform.WEB)
                rollout = Rollout.of(10.0)
            } implies true
        }
    }

    fun testPlatform(platform: Platform, label: String) {
        val results = (1..100).map { userId ->
            val context = Context(
                locale = AppLocale.EN_US,
                platform = platform,
                appVersion = Version(1, 0, 0),
                stableId = StableId.of("user-$userId")
            )
            context.evaluate(EXPERIMENTAL_FEATURE)
        }
        val enabledCount = results.count { it }
        println("$label: $enabledCount/100 users")
    }

    testPlatform(Platform.IOS, "iOS")
    testPlatform(Platform.ANDROID, "Android")
    testPlatform(Platform.WEB, "Web")
}

/**
 * Example 4: Deterministic Bucketing
 *
 * Same user always gets the same bucket (deterministic).
 */
fun deterministicBucketing() {
    val AB_TEST: Conditional<Boolean, Context> =
        Conditional("ab_test")

    ConfigBuilder.config {
        AB_TEST with {
            default(value = false)

            rule {
                rollout = Rollout.of(50.0)
            } implies true
        }
    }

    // Same user, evaluated multiple times
    val userId = "consistent-user-123"

    val results = (1..10).map {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of(userId)
        )
        context.evaluate(AB_TEST)
    }

    // All evaluations should return the same result
    val uniqueResults = results.distinct()
    println("User '$userId' evaluated 10 times")
    println("Unique results: ${uniqueResults.size} (should be 1)")
    println("Consistent result: ${results.first()}")
}

/**
 * Example 5: Multi-Variant Testing
 *
 * Distribute users across multiple variants using rollout.
 */
fun multiVariantTesting() {
    enum class CheckoutVariant {
        CONTROL, VARIANT_A, VARIANT_B, VARIANT_C
    }

    val CHECKOUT_VARIANT: Conditional<CheckoutVariant, Context> =
        Conditional("checkout_variant")

    ConfigBuilder.config {
        CHECKOUT_VARIANT with {
            default(value = CheckoutVariant.CONTROL)

            // 25% get Variant A
            rule {
                note("Variant A: Simplified checkout")
                rollout = Rollout.of(25.0)
            } implies CheckoutVariant.VARIANT_A

            // 50% total get Variant B (25% additional)
            rule {
                note("Variant B: One-click checkout")
                rollout = Rollout.of(50.0)
            } implies CheckoutVariant.VARIANT_B

            // 75% total get Variant C (25% additional)
            rule {
                note("Variant C: Express checkout")
                rollout = Rollout.of(75.0)
            } implies CheckoutVariant.VARIANT_C

            // Remaining 25% get control (default)
        }
    }

    val results = (1..1000).map { userId ->
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("user-$userId")
        )
        context.evaluate(CHECKOUT_VARIANT)
    }

    val distribution = results.groupingBy { it }.eachCount()
    println("Distribution across 1000 users:")
    distribution.forEach { (variant, count) ->
        val percentage = (count.toDouble() / 10.0).toInt()
        println("$variant: $count users (~$percentage%)")
    }
}

/**
 * Example 6: Salt-Based Bucketing
 *
 * Use different salts to re-bucket users for different experiments.
 */
fun saltBasedBucketing() {
    val EXPERIMENT_V1: Conditional<Boolean, Context> =
        Conditional("experiment_v1")

    val EXPERIMENT_V2: Conditional<Boolean, Context> =
        Conditional("experiment_v2")

    ConfigBuilder.config {
        EXPERIMENT_V1 with {
            default(value = false)
            salt("v1")  // Salt v1

            rule {
                rollout = Rollout.of(50.0)
            } implies true
        }

        EXPERIMENT_V2 with {
            default(value = false)
            salt("v2")  // Different salt v2

            rule {
                rollout = Rollout.of(50.0)
            } implies true
        }
    }

    // Same user evaluated against different salts
    val userId = "test-user"
    val context = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of(userId)
    )

    val v1Result = context.evaluate(EXPERIMENT_V1)
    val v2Result = context.evaluate(EXPERIMENT_V2)

    println("User '$userId' with salt v1: $v1Result")
    println("User '$userId' with salt v2: $v2Result")
    println("Different salts can produce different buckets")
}

fun main() {
    gradualRollout()
    println("\n" + "=".repeat(50) + "\n")
    phasedRollout()
    println("\n" + "=".repeat(50) + "\n")
    platformSpecificRollout()
    println("\n" + "=".repeat(50) + "\n")
    deterministicBucketing()
    println("\n" + "=".repeat(50) + "\n")
    multiVariantTesting()
    println("\n" + "=".repeat(50) + "\n")
    saltBasedBucketing()
}
