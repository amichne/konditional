package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.example.PaymentFeatures
import io.amichne.konditional.example.SearchFeatures
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingletonModuleRegistryTests {
    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "7.12.3",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @BeforeTest
    fun loadSample() {
        object : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
            val FIFTY_TRUE_US_IOS by boolean<Context>("") {
                default(false)
                rule {
                    platforms(Platform.IOS)
                    versions {
                        min(7, 10, 0)
                    }
                } implies true
            }
            val DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY by boolean("") {
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
        val a = ctx(id).evaluate(PaymentFeatures.FIFTY_TRUE_US_IOS)
        repeat(100) {
            assertEquals(a, ctx(id).evaluate(PaymentFeatures.FIFTY_TRUE_US_IOS))
        }
    }

    @Test
    fun `Given same Id, When evaluating different flags, Then results are independent`() {
        val id = "ffffffffffffffffffffffffffffffff"
        val a = ctx(id).evaluate(PaymentFeatures.FIFTY_TRUE_US_IOS)
        val b = ctx(id).evaluate(SearchFeatures.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY)
        assertEquals(a, ctx(id).evaluate(PaymentFeatures.FIFTY_TRUE_US_IOS))
        assertEquals(b, ctx(id).evaluate(SearchFeatures.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY))
    }

    @Test
    fun `Given multiple rules, When specificity differs, Then most specific rule wins`() {
            SearchFeatures.PRIORITY_CHECK.update {
                default(false)
                rule {
                } implies true
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
        val id = "0123456789abcdef0123456789abcdef"
        val result = ctx(id).evaluate(SearchFeatures.PRIORITY_CHECK)
        assertTrue(result)
    }

    @Test
    fun `Given version bounds, When inclusive, Then correctly matches edges`() {
        Taxonomy.Core.config {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10, 0)
                        max(7, 12, 3)
                    }
                } implies true
            }
        }

        assertTrue(ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", version = "7.10.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", version = "7.12.3").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("cccccccccccccccccccccccccccccccc", version = "7.12.4").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given at least major version, When evaluating, Then correctly matches range`() {
        // >= 7.0.0
        Taxonomy.Core.config<Taxonomy.Core> // >= 7.0.0
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7) // >= 7.0.0
                    }
                } implies true
            }
        }

        // Below minimum
        assertFalse(ctx("10000000000000000000000000000001", version = "6.99.99").evaluate(SearchFeatures.VERSIONED))
        // Exactly at minimum
        assertTrue(ctx("10000000000000000000000000000002", version = "7.0.0").evaluate(SearchFeatures.VERSIONED))
        // Above minimum
        assertTrue(ctx("10000000000000000000000000000003", version = "7.0.1").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("10000000000000000000000000000004", version = "7.1.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("10000000000000000000000000000005", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given at least major minor version, When evaluating, Then correctly matches range`() {
        // >= 7.10.0
        Taxonomy.Core.config<Taxonomy.Core> // >= 7.10.0
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10) // >= 7.10.0
                    }
                } implies true
            }
        }

        // Below minimum
        assertFalse(ctx("20000000000000000000000000000001", version = "7.9.99").evaluate(SearchFeatures.VERSIONED))
        // Exactly at minimum
        assertTrue(ctx("20000000000000000000000000000002", version = "7.10.0").evaluate(SearchFeatures.VERSIONED))
        // Above minimum
        assertTrue(ctx("20000000000000000000000000000003", version = "7.10.1").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("20000000000000000000000000000004", version = "7.11.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("20000000000000000000000000000005", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given at least major minor patch version, When evaluating, Then correctly matches range`() {
        // >= 7.10.5
        Taxonomy.Core.config<Taxonomy.Core> // >= 7.10.5
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10, 5) // >= 7.10.5
                    }
                } implies true
            }
        }

        // Below minimum
        assertFalse(ctx("30000000000000000000000000000001", version = "7.10.4").evaluate(SearchFeatures.VERSIONED))
        // Exactly at minimum
        assertTrue(ctx("30000000000000000000000000000002", version = "7.10.5").evaluate(SearchFeatures.VERSIONED))
        // Above minimum
        assertTrue(ctx("30000000000000000000000000000003", version = "7.10.6").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("30000000000000000000000000000004", version = "7.11.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("30000000000000000000000000000005", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given at most major version, When evaluating, Then correctly matches range`() {
        // <= 7.0.0
        Taxonomy.Core.config<Taxonomy.Core> // <= 7.0.0
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7) // <= 7.0.0
                    }
                } implies true
            }
        }

        // Below maximum
        assertTrue(ctx("40000000000000000000000000000001", version = "6.99.99").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("40000000000000000000000000000002", version = "6.0.0").evaluate(SearchFeatures.VERSIONED))
        // Exactly at maximum
        assertTrue(ctx("40000000000000000000000000000003", version = "7.0.0").evaluate(SearchFeatures.VERSIONED))
        // Above maximum
        assertFalse(ctx("40000000000000000000000000000004", version = "7.0.1").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("40000000000000000000000000000005", version = "7.1.0").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("40000000000000000000000000000006", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given at most major minor version, When evaluating, Then correctly matches range`() {
        // <= 7.10.0
        Taxonomy.Core.config<Taxonomy.Core> // <= 7.10.0
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7, 10) // <= 7.10.0
                    }
                } implies true
            }
        }

        // Below maximum
        assertTrue(ctx("50000000000000000000000000000001", version = "7.9.99").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("50000000000000000000000000000002", version = "6.0.0").evaluate(SearchFeatures.VERSIONED))
        // Exactly at maximum
        assertTrue(ctx("50000000000000000000000000000003", version = "7.10.0").evaluate(SearchFeatures.VERSIONED))
        // Above maximum
        assertFalse(ctx("50000000000000000000000000000004", version = "7.10.1").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("50000000000000000000000000000005", version = "7.11.0").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("50000000000000000000000000000006", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given at most major minor patch version, When evaluating, Then correctly matches range`() {
        // <= 7.10.5
        Taxonomy.Core.config<Taxonomy.Core> // <= 7.10.5
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7, 10, 5) // <= 7.10.5
                    }
                } implies true
            }
        }

        // Below maximum
        assertTrue(ctx("60000000000000000000000000000001", version = "7.10.4").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("60000000000000000000000000000002", version = "6.0.0").evaluate(SearchFeatures.VERSIONED))
        // Exactly at maximum
        assertTrue(ctx("60000000000000000000000000000003", version = "7.10.5").evaluate(SearchFeatures.VERSIONED))
        // Above maximum
        assertFalse(ctx("60000000000000000000000000000004", version = "7.10.6").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("60000000000000000000000000000005", version = "7.11.0").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("60000000000000000000000000000006", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given combined version granularities, When evaluating, Then correctly matches range`() {
        // >= 5.0.0
        Taxonomy // <= 7.10.5
            .Core.config<Taxonomy.Core> // >= 5.0.0 // <= 7.10.5
            {
                SearchFeatures.VERSIONED with {
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
        assertFalse(ctx("70000000000000000000000000000001", version = "4.99.99").evaluate(SearchFeatures.VERSIONED))
        // At lower bound
        assertTrue(ctx("70000000000000000000000000000002", version = "5.0.0").evaluate(SearchFeatures.VERSIONED))
        // Within range
        assertTrue(ctx("70000000000000000000000000000003", version = "6.0.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("70000000000000000000000000000004", version = "7.10.0").evaluate(SearchFeatures.VERSIONED))
        // At upper bound
        assertTrue(ctx("70000000000000000000000000000005", version = "7.10.5").evaluate(SearchFeatures.VERSIONED))
        // Above range
        assertFalse(ctx("70000000000000000000000000000006", version = "7.10.6").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("70000000000000000000000000000007", version = "8.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given open ended minimum version, When evaluating, Then correctly matches range`() {
        // >= 7.10.0, no maximum
        Taxonomy.Core.config<Taxonomy.Core> // >= 7.10.0, no maximum
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        min(7, 10) // >= 7.10.0, no maximum
                    }
                } implies true
            }
        }

        assertFalse(ctx("80000000000000000000000000000001", version = "7.9.99").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("80000000000000000000000000000002", version = "7.10.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("80000000000000000000000000000003", version = "10.0.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("80000000000000000000000000000004", version = "100.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given open ended maximum version, When evaluating, Then correctly matches range`() {
        // <= 7.10.0, no minimum
        Taxonomy.Core.config<Taxonomy.Core> // <= 7.10.0, no minimum
        {
            SearchFeatures.VERSIONED with {
                default(false)
                rule {
                    versions {
                        max(7, 10) // <= 7.10.0, no minimum
                    }
                } implies true
            }
        }

        assertTrue(ctx("90000000000000000000000000000001", version = "1.0.0").evaluate(SearchFeatures.VERSIONED))
        assertTrue(ctx("90000000000000000000000000000002", version = "7.10.0").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("90000000000000000000000000000003", version = "7.10.1").evaluate(SearchFeatures.VERSIONED))
        assertFalse(ctx("90000000000000000000000000000004", version = "10.0.0").evaluate(SearchFeatures.VERSIONED))
    }

    @Test
    fun `Given uniform bucket distribution, When evaluating, Then distribution is reasonable`() {
        Taxonomy.Core.config {
            SearchFeatures.UNIFORM50 with {
                default(false)
                rule {
                    rollout { 50.0 }
                } implies true
            }
        }

        val times = 10000
        var trues = 0
        repeat(times) {
            val id = Random.nextBytes(16).joinToString("") { "%02x".format(it) }
            if (ctx(id).evaluate(SearchFeatures.UNIFORM50)) trues++
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
                    ctx(id).evaluate(PaymentFeatures.FIFTY_TRUE_US_IOS)
                }
            }
        repeat(6) { pool.submit(reader) }

        val writer =
            Runnable {
                latch.await()
                repeat(50) {
                    Taxonomy.Core.config {
                        PaymentFeatures.FIFTY_TRUE_US_IOS with {
                            default(false)
                            rule {
                                platforms(
                                    Platform.IOS
                                )
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
        Taxonomy.Core.config {
            PaymentFeatures.ENABLE_COMPACT_CARDS with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
            PaymentFeatures.USE_LIGHTWEIGHT_HOME with {
                default(true)
            }
        }

        val id = "0123456789abcdef0123456789abcdef"
        // These calls are type-safe - cannot pass a string or invalid key
        val result1 = ctx(id).evaluate(PaymentFeatures.ENABLE_COMPACT_CARDS)
        val result2 = ctx(id).evaluate(PaymentFeatures.USE_LIGHTWEIGHT_HOME)

        assertTrue(result1) // Should be true for US iOS at 100% coverage
        assertTrue(result2) // Should be true (default true)
    }
}
