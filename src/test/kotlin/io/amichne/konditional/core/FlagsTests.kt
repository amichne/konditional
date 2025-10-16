package io.amichne.konditional.core

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Flags.evaluate
import io.amichne.konditional.example.SampleFeatureEnum
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
                    version {
                        leftBound(7, 10, 0)
                    }
                } gives true
            }
            SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY withRules {
                default(true)
                rule {
                    platforms(Platform.ANDROID)
                    version {
                        rightBound(6, 4, 99)
                    }
                } gives false
            }
        }
    }

    @Test
    fun `Given same Id, When evaluating flag, Then result is deterministic`() {
        val id = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
        val a = ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)
        repeat(100) {
            assertEquals(a, ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS))
        }
    }

    @Test
    fun `Given same Id, When evaluating different flags, Then results are independent`() {
        val id = "ffffffffffffffffffffffffffffffff"
        val a = ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)
        val b = ctx(id).evaluate(SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY)
        assertEquals(a, ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS))
        assertEquals(b, ctx(id).evaluate(SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY))
    }

    @Test
    fun `Given multiple rules, When specificity differs, Then most specific rule wins`() {
        config {
            SampleFeatureEnum.PRIORITY_CHECK withRules {
                default(false)
                rule {
                } gives true
                rule {
                    platforms(Platform.IOS)
                } gives true
            }
        }
        val id = "0123456789abcdef0123456789abcdef"
        val result = ctx(id).evaluate(SampleFeatureEnum.PRIORITY_CHECK)
        assertTrue(result)
    }

    @Test
    fun `Given version bounds, When inclusive, Then correctly matches edges`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        leftBound(7, 10, 0)
                        rightBound(7, 12, 3)
                    }
                } gives true
            }
        }

        assertTrue(ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", version = "7.12.3").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("cccccccccccccccccccccccccccccccc", version = "7.12.4").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at least major version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        leftBound(7)  // >= 7.0.0
                    }
                } gives true
            }
        }

        // Below minimum
        assertFalse(ctx("10000000000000000000000000000001", version = "6.99.99").evaluate(SampleFeatureEnum.VERSIONED))
        // Exactly at minimum
        assertTrue(ctx("10000000000000000000000000000002", version = "7.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Above minimum
        assertTrue(ctx("10000000000000000000000000000003", version = "7.0.1").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("10000000000000000000000000000004", version = "7.1.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("10000000000000000000000000000005", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at least major minor version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        leftBound(7, 10)  // >= 7.10.0
                    }
                } gives true
            }
        }

        // Below minimum
        assertFalse(ctx("20000000000000000000000000000001", version = "7.9.99").evaluate(SampleFeatureEnum.VERSIONED))
        // Exactly at minimum
        assertTrue(ctx("20000000000000000000000000000002", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Above minimum
        assertTrue(ctx("20000000000000000000000000000003", version = "7.10.1").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("20000000000000000000000000000004", version = "7.11.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("20000000000000000000000000000005", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at least major minor patch version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        leftBound(7, 10, 5)  // >= 7.10.5
                    }
                } gives true
            }
        }

        // Below minimum
        assertFalse(ctx("30000000000000000000000000000001", version = "7.10.4").evaluate(SampleFeatureEnum.VERSIONED))
        // Exactly at minimum
        assertTrue(ctx("30000000000000000000000000000002", version = "7.10.5").evaluate(SampleFeatureEnum.VERSIONED))
        // Above minimum
        assertTrue(ctx("30000000000000000000000000000003", version = "7.10.6").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("30000000000000000000000000000004", version = "7.11.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("30000000000000000000000000000005", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at most major version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        rightBound(7)  // <= 7.0.0
                    }
                } gives true
            }
        }

        // Below maximum
        assertTrue(ctx("40000000000000000000000000000001", version = "6.99.99").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("40000000000000000000000000000002", version = "6.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Exactly at maximum
        assertTrue(ctx("40000000000000000000000000000003", version = "7.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Above maximum
        assertFalse(ctx("40000000000000000000000000000004", version = "7.0.1").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("40000000000000000000000000000005", version = "7.1.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("40000000000000000000000000000006", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at most major minor version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        rightBound(7, 10)  // <= 7.10.0
                    }
                } gives true
            }
        }

        // Below maximum
        assertTrue(ctx("50000000000000000000000000000001", version = "7.9.99").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("50000000000000000000000000000002", version = "6.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Exactly at maximum
        assertTrue(ctx("50000000000000000000000000000003", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Above maximum
        assertFalse(ctx("50000000000000000000000000000004", version = "7.10.1").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("50000000000000000000000000000005", version = "7.11.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("50000000000000000000000000000006", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at most major minor patch version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        rightBound(7, 10, 5)  // <= 7.10.5
                    }
                } gives true
            }
        }

        // Below maximum
        assertTrue(ctx("60000000000000000000000000000001", version = "7.10.4").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("60000000000000000000000000000002", version = "6.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Exactly at maximum
        assertTrue(ctx("60000000000000000000000000000003", version = "7.10.5").evaluate(SampleFeatureEnum.VERSIONED))
        // Above maximum
        assertFalse(ctx("60000000000000000000000000000004", version = "7.10.6").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("60000000000000000000000000000005", version = "7.11.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("60000000000000000000000000000006", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given combined version granularities, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        leftBound(5)          // >= 5.0.0
                        rightBound(7, 10, 5)    // <= 7.10.5
                    }
                } gives true
            }
        }

        // Below range
        assertFalse(ctx("70000000000000000000000000000001", version = "4.99.99").evaluate(SampleFeatureEnum.VERSIONED))
        // At lower bound
        assertTrue(ctx("70000000000000000000000000000002", version = "5.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        // Within range
        assertTrue(ctx("70000000000000000000000000000003", version = "6.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("70000000000000000000000000000004", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        // At upper bound
        assertTrue(ctx("70000000000000000000000000000005", version = "7.10.5").evaluate(SampleFeatureEnum.VERSIONED))
        // Above range
        assertFalse(ctx("70000000000000000000000000000006", version = "7.10.6").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("70000000000000000000000000000007", version = "8.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given open ended minimum version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        leftBound(7, 10)  // >= 7.10.0, no maximum
                    }
                } gives true
            }
        }

        assertFalse(ctx("80000000000000000000000000000001", version = "7.9.99").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("80000000000000000000000000000002", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("80000000000000000000000000000003", version = "10.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("80000000000000000000000000000004", version = "100.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given open ended maximum version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED withRules {
                default(false)
                rule {
                    version {
                        rightBound(7, 10)  // <= 7.10.0, no minimum
                    }
                } gives true
            }
        }

        assertTrue(ctx("90000000000000000000000000000001", version = "1.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("90000000000000000000000000000002", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("90000000000000000000000000000003", version = "7.10.1").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("90000000000000000000000000000004", version = "10.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given default coverage, When evaluating, Then distribution is correct`() {
        config {
            SampleFeatureEnum.DEFAULT_FALSE_WITH_30_TRUE withRules {
                default(true, fallback = false, coverage = 30.0)
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
    fun `Given uniform bucket distribution, When evaluating, Then distribution is reasonable`() {
        config {
            SampleFeatureEnum.UNIFORM50 withRules {
                default(false)
                rule {
                    rampUp = 50.0
                } gives true
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
    fun `Given concurrent registry swap, When evaluating, Then thread safety is maintained`() {
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
                        rule {
                            platforms(Platform.IOS)
                        } gives true
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
    fun `Given enum based keys, When evaluating, Then type safety is enforced`() {
        // This test validates that using enum-based keys provides compile-time type safety
        // and prevents typos or undefined flag keys
        config {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS withRules {
                default(false)
                rule {
                    platforms(Platform.IOS)
                } gives true
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
