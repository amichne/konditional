package io.amichne.konditional.flags

import kotlin.test.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FlagsTests {

    private fun ctx(idHex: String, locale: AppLocale = AppLocale.EN_US,
                    platform: Platform = Platform.IOS, version: String = "7.12.3") =
        EvalContext(locale, platform, Version.parse(version), StableId.of(idHex))

    @BeforeTest
    fun loadSample() {
        val reg = ConfigBuilder().apply {
            flag("fifty_true_us_ios") {
                default(false)
                rule {
                    markets(Market.US); platforms(Platform.IOS); versions(min="7.10.0")
                    value(true, coveragePct = 50.0)
                }
            }
            flag("default_true_except_android_legacy") {
                default(true)
                rule {
                    platforms(Platform.ANDROID); versions(max = "6.4.99")
                    value(false, coveragePct = 100.0)
                }
            }
        }.build()
        Flags.load(reg)
    }

    @Test
    fun determinism_same_id_same_result() {
        val id = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
        val a = Flags.eval("fifty_true_us_ios", ctx(id))
        repeat(100) {
            assertEquals(a, Flags.eval("fifty_true_us_ios", ctx(id)))
        }
    }

    @Test
    fun independence_across_flags() {
        val id = "ffffffffffffffffffffffffffffffff"
        // Different keys imply independent buckets; results can differ
        val a = Flags.eval("fifty_true_us_ios", ctx(id))
        val b = Flags.eval("default_true_except_android_legacy", ctx(id))
        // Not asserting equality; asserting both calls succeed deterministically
        assertEquals(a, Flags.eval("fifty_true_us_ios", ctx(id)))
        assertEquals(b, Flags.eval("default_true_except_android_legacy", ctx(id)))
    }

    @Test
    fun specificity_priority_and_fallthrough() {
        val reg = ConfigBuilder().apply {
            flag("priority_check") {
                default(false)
                // Broad rule: US any platform true 10%
                rule { markets(Market.US); value(true, coveragePct = 10.0) }
                // More specific: US + iOS true 100%
                rule { markets(Market.US); platforms(Platform.IOS); value(true, coveragePct = 100.0) }
            }
        }.build()
        Flags.load(reg)
        val id = "0123456789abcdef0123456789abcdef"
        val result = Flags.eval("priority_check", ctx(id))
        assertTrue(result) // specific 100% rule should win
    }

    @Test
    fun version_bounds_inclusive() {
        val reg = ConfigBuilder().apply {
            flag("versioned") {
                default(false)
                rule { versions(min="7.10.0", max="7.12.3"); value(true, coveragePct = 100.0) }
            }
        }.build()
        Flags.load(reg)
        assertTrue(Flags.eval("versioned", ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", version="7.10.0")))
        assertTrue(Flags.eval("versioned", ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", version="7.12.3")))
        assertFalse(Flags.eval("versioned", ctx("cccccccccccccccccccccccccccccccc", version="7.12.4")))
    }

    @Test
    fun default_coverage_behaviour() {
        val reg = ConfigBuilder().apply {
            flag("default_false_with_30_true") {
                default(false, coverage = 30.0)
            }
        }.build()
        Flags.load(reg)

        // Sample many users; expect about 30% true (+/- small tolerance)
        val N = 5000
        var trues = 0
        for (i in 0 until N) {
            val id = "%032x".format(i)
            if (Flags.eval("default_false_with_30_true",
                    ctx(id, version = "1.0.0"))) trues++
        }
        val pct = trues.toDouble() / N
        assertTrue(pct in 0.27..0.33, "Observed $pct")
    }

    @Test
    fun bucket_uniformity_is_reasonable() {
        val reg = ConfigBuilder().apply {
            flag("uniform50") {
                default(false)
                rule { value(true, coveragePct = 50.0) }
            }
        }.build()
        Flags.load(reg)

        val N = 10000
        var trues = 0
        for (i in 0 until N) {
            val id = Random.nextBytes(16).joinToString("") { "%02x".format(it) }
            if (Flags.eval("uniform50", ctx(id))) trues++
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
                Flags.eval("fifty_true_us_ios", ctx(id))
            }
        }
        repeat(6) { pool.submit(reader) }

        val writer = Runnable {
            latch.await()
            repeat(50) {
                val reg = ConfigBuilder().apply {
                    flag("fifty_true_us_ios") {
                        default(false)
                        rule { markets(Market.US); platforms(Platform.IOS); value(true, coveragePct = 50.0) }
                    }
                }.build()
                Flags.load(reg)
            }
        }
        pool.submit(writer)
        latch.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
    }
}
