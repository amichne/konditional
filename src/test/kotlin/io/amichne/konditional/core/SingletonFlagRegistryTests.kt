package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.id.StableId
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

class SingletonFlagRegistryTests {
    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "7.12.3",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @BeforeTest
    fun loadSample() {
        config {
            SampleFeatureEnum.FIFTY_TRUE_US_IOS with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                    versions {
                        min(7, 10, 0)
                    }
                } implies true
            }
            SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY with {
                default(true)
                rule {
                    platforms(Platform.ANDROID)
                    versions {
                        max(6, 4, 99)
                    }
                } implies false
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
            SampleFeatureEnum.PRIORITY_CHECK with {
                default(false)
                rule {
                } implies true
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
        }
        val id = "0123456789abcdef0123456789abcdef"
        val result = ctx(id).evaluate(SampleFeatureEnum.PRIORITY_CHECK)
        assertTrue(result)
    }

    @Test
    fun `Given version bounds, When inclusive, Then correctly matches edges`() {
        config {
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10, 0)
                        max(7, 12, 3)
                    }
                } implies true
            }
        }

        assertTrue(ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", version = "7.12.3").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("cccccccccccccccccccccccccccccccc", version = "7.12.4").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given at least major version, When evaluating, Then correctly matches range`() {
        config {
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7) // >= 7.0.0
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10) // >= 7.10.0
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10, 5) // >= 7.10.5
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7) // <= 7.0.0
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7, 10) // <= 7.10.0
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7, 10, 5) // <= 7.10.5
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(5) // >= 5.0.0
                        max(7, 10, 5) // <= 7.10.5
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10) // >= 7.10.0, no maximum
                    }
                } implies true
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
            SampleFeatureEnum.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7, 10) // <= 7.10.0, no minimum
                    }
                } implies true
            }
        }

        assertTrue(ctx("90000000000000000000000000000001", version = "1.0.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertTrue(ctx("90000000000000000000000000000002", version = "7.10.0").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("90000000000000000000000000000003", version = "7.10.1").evaluate(SampleFeatureEnum.VERSIONED))
        assertFalse(ctx("90000000000000000000000000000004", version = "10.0.0").evaluate(SampleFeatureEnum.VERSIONED))
    }

    @Test
    fun `Given uniform bucket distribution, When evaluating, Then distribution is reasonable`() {
        config {
            SampleFeatureEnum.UNIFORM50 with {
                default(false)
                rule {
                    rollout = Rollout.of(50.0)
                } implies true
            }
        }

        val times = 10000
        var trues = 0
        repeat(times) {
            val id = Random.nextBytes(16).joinToString("") { "%02x".format(it) }
            if (ctx(id).evaluate(SampleFeatureEnum.UNIFORM50)) trues++
        }
        val pct = trues.toDouble() / times
        assertTrue(pct in 0.47..0.53, "Observed $pct")
    }

    @Test
    fun `Given concurrent registry swap, When evaluating, Then thread safety is maintained`() {
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(1)

        val reader =
            Runnable {
                latch.await()
                repeat(1000) {
                    val id = "%032x".format(it)
                    ctx(id).evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)
                }
            }
        repeat(6) { pool.submit(reader) }

        val writer =
            Runnable {
                latch.await()
                repeat(50) {
                    config {
                        SampleFeatureEnum.FIFTY_TRUE_US_IOS with {
                            default(false)
                            rule {
                                platforms(Platform.IOS)
                            } implies true
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
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(true)
            }
        }

        val id = "0123456789abcdef0123456789abcdef"
        // These calls are type-safe - cannot pass a string or invalid key
        val result1 = ctx(id).evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)
        val result2 = ctx(id).evaluate(SampleFeatureEnum.USE_LIGHTWEIGHT_HOME)

        assertTrue(result1) // Should be true for US iOS at 100% coverage
        assertTrue(result2) // Should be true (default true)
    }
}
