package io.amichne.konditional.core

import io.amichne.konditional.core.context.AppLocale
import io.amichne.konditional.core.context.Context
import io.amichne.konditional.core.context.Platform
import io.amichne.konditional.core.context.Version
import io.amichne.konditional.core.ConfigBuilder.Companion.config
import io.amichne.konditional.core.Flags.evaluate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.*

class FlagsTests {

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "7.12.3"
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @BeforeTest
    fun loadSample() {
        config {
            SampleFeatureEnum.FIFTY_TRUE_US_IOS withRules {
                default(false)
                rule {
                    platforms(Platform.IOS)
                    versions(min = "7.10.0")
                    value(true, coveragePct = 50.0)
                }
            }
            SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY withRules {
                default(true)
                rule {
                    platforms(Platform.ANDROID); versions(max = "6.4.99")
                    value(false, coveragePct = 100.0)
                }
            }
        }
    }

    @Test
    fun determinism_same_id_same_result() {
        val id = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
        val a = ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)
        repeat(100) {
            assertEquals(a, ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS))
        }
    }

    @Test
    fun independence_across_flags() {
        val id = "ffffffffffffffffffffffffffffffff"
        // Different keys imply independent buckets; results can differ
        val a = ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)
        val b = ctx(id).evaluate(SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY)
        // Not asserting equality; asserting both calls succeed deterministically
        assertEquals(a, ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS))
        assertEquals(b, ctx(id).evaluate(SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY))
    }

    @Test
    fun specificity_priority_and_fallthrough() {
        config {
            SampleFeatureEnum.PRIORITY_CHECK withRules {
                default(false)
                // Broad rule: US any platform true 10%
                rule {
                    value(true, coveragePct = 10.0)
                }
                // More specific: US + iOS true 100%
                rule {
                    platforms(Platform.IOS);
                    value(true, coveragePct = 100.0)
                }
            }
        }
        val id = "0123456789abcdef0123456789abcdef"
        val result = ctx(id).evaluate(SampleFeatureEnum.PRIORITY_CHECK)
        assertTrue(result) // specific 100% rule should win
    }

    @Test
    fun version_bounds_inclusive() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule { versions(min = "7.10.0", max = "7.12.3"); value(true, coveragePct = 100.0) }
            }
        }

        assertTrue(ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", version = "7.12.3").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("cccccccccccccccccccccccccccccccc", version = "7.12.4").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun default_coverage_behaviour() {
        config {
            SampleFeatureEnum.DEFAULT_FALSE_WITH_30_TRUE withRules {
                default(false, coverage = 30.0)
            }
        }

        // Sample many users; expect about 30% true (+/- small tolerance)
        val N = 5000
        var trues = 0
        for (i in 0 until N) {
            val id = "%032x".format(i)
            if (ctx(id, version = "1.0.0").evaluate(
                    SampleFeatureEnum.DEFAULT_FALSE_WITH_30_TRUE
                )
            ) trues++
        }
        val pct = trues.toDouble() / N
        assertTrue(pct in 0.27..0.33, "Observed $pct")
    }

    @Test
    fun bucket_uniformity_is_reasonable() {
        config {
            SampleFeatureEnum.UNIFORM50 withRules {
                default(false)
                rule { value(true, coveragePct = 50.0) }
            }
        }

        val N = 10000
        var trues = 0
        for (i in 0 until N) {
            val id = Random.nextBytes(16).joinToString("") { "%02x".format(it) }
            if (ctx(id).evaluate(SampleFeatureEnum.UNIFORM50)) trues++
        }
        val pct = trues.toDouble() / N
        assertTrue(pct in 0.47..0.53, "Observed $pct")
    }

    @Test
    fun thread_safe_registry_swap() {
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(1)

        val reader = Runnable {
            latch.await()
            repeat(1000) {
                val id = "%032x".format(it)
                ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)
            }
        }
        repeat(6) { pool.submit(reader) }

        val writer = Runnable {
            latch.await()
            repeat(50) {
                config {
                    SampleFeatureEnum.FIFTY_TRUE_US_IOS withRules {
                        default(false)
                        rule { platforms(Platform.IOS); value(true, coveragePct = 50.0) }
                    }
                }
            }
        }
        pool.submit(writer)
        latch.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
    }

    @Test
    fun enum_based_keys_provide_type_safety() {
        // This test validates that using enum-based keys provides compile-time type safety
        // and prevents typos or undefined flag keys
        config {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS withRules {
                default(false)
                rule {
                    platforms(Platform.IOS)
                    value(true, coveragePct = 100.0)
                }
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME withRules {
                default(true)
            }
        }

        val id = "0123456789abcdef0123456789abcdef"
        // These calls are type-safe - cannot pass a string or invalid key
        val result1 = ctx(id).evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)
        val result2 = ctx(id).evaluate(SampleFeatureEnum.USE_LIGHTWEIGHT_HOME)

        assertTrue(result1) // Should be true for US iOS at 100% coverage
        assertTrue(result2) // Should be true (default true)

        // Verify evaluate returns a map withRules FeatureFlag keys
        val allResults = ctx(id).evaluate()
        assertTrue(allResults.containsKey(SampleFeatureEnum.ENABLE_COMPACT_CARDS))
        assertTrue(allResults.containsKey(SampleFeatureEnum.USE_LIGHTWEIGHT_HOME))
        assertEquals(2, allResults.size)
    }
}
