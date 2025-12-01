package io.amichne.konditional.adversarial

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.id.StableId
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CONCURRENCY ATTACK TESTS
 *
 * Tests that attempt to exploit race conditions, thread safety issues,
 * and concurrent access patterns that could break the system.
 *
 * The library claims to be thread-safe with "lock-free reads with atomic updates".
 * Let's test if that holds under adversarial concurrent access.
 */
class ConcurrencyAttacksTest {

    // ============================================
    // ATTACK 1: Concurrent Feature Registration
    // ============================================

    @Test
    fun `ATTACK - concurrent access to lazy feature registration`() {
        /*
         * ATTACK: Access features concurrently before they're registered
         * RESULT: Test if lazy registration is thread-safe
         * DANGER: Race condition during first access could cause:
         *         - Multiple registrations
         *         - Partial initialization
         *         - Inconsistent state
         */

        val container = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val concurrentFeature1 by boolean<Context>(default = true)
            val concurrentFeature2 by boolean<Context>(default = false)
            val concurrentFeature3 by string<Context>(default = "test")
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        // 100 threads all try to access features concurrently
        repeat(100) { threadNum ->
            executor.submit {
                try {
                    when (threadNum % 3) {
                        0 -> container.concurrentFeature1
                        1 -> container.concurrentFeature2
                        2 -> container.concurrentFeature3
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Check for race condition errors
        if (errors.isNotEmpty()) {
            println("FOUND CONCURRENCY BUGS:")
            errors.forEach { it.printStackTrace() }
        }

        assertTrue(errors.isEmpty(), "Concurrent registration caused errors: $errors")

        // Verify all features registered exactly once
        assertEquals(3, container.allFeatures().size)
    }

    // ============================================
    // ATTACK 2: Concurrent Evaluation Under Load
    // ============================================

    @Test
    fun `ATTACK - concurrent flag evaluation with different contexts`() {
        /*
         * ATTACK: Hammer flag evaluation with many threads
         * RESULT: Test thread-safety of evaluation logic
         * DANGER: Race conditions in:
         *         - Rule matching
         *         - Bucket calculation (SHA-256 digest)
         *         - Value retrieval
         */

        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val highContentionFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.ANDROID)
                    rollout { 50.0 }
                } returns true

                rule {
                    platforms(Platform.IOS)
                    rollout { 30.0 }
                } returns true
            }
        }

        val executor = Executors.newFixedThreadPool(20)
        val latch = CountDownLatch(1000)
        val results = ConcurrentHashMap<Int, Boolean>()
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        // 1000 evaluations across 20 threads
        repeat(1000) { i ->
            executor.submit {
                try {
                    val context = Context(
                        locale = AppLocale.EN_US,
                        platform = if (i % 2 == 0) Platform.ANDROID else Platform.IOS,
                        appVersion = Version(1, 0, 0),
                        stableId = StableId.of(String.format("%032d", i))
                    )

                    val result = TestFeatures.highContentionFlag.evaluate(context)
                    results[i] = result
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue(errors.isEmpty(), "Concurrent evaluation caused errors: $errors")
        assertEquals(1000, results.size, "Some evaluations were lost")

        // Verify determinism: same context always gives same result
        val context1 = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of(String.format("%032d", 0))
        )

        val result1 = TestFeatures.highContentionFlag.evaluate(context1)
        val result2 = TestFeatures.highContentionFlag.evaluate(context1)
        assertEquals(result1, result2, "Non-deterministic evaluation detected")
    }

    // ============================================
    // ATTACK 3: SHA-256 MessageDigest Thread Safety
    // ============================================

    @Test
    fun `ATTACK - concurrent SHA-256 digest usage in bucketing`() {
        /*
         * ATTACK: Test if MessageDigest.getInstance is thread-safe
         * RESULT: MessageDigest is NOT thread-safe if shared
         * DANGER: If FlagDefinition.shaDigestSpi is shared across threads,
         *         concurrent digest() calls will corrupt each other
         *
         * NOTE: MessageDigest.getInstance("SHA-256") returns a NEW instance,
         *       but the code has: "val shaDigestSpi: MessageDigest"
         *       If this is a singleton, it's a CRITICAL BUG
         */

        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            // Create many flags to stress digest usage
            val flag1 by boolean<Context>(default = false) {
                rule { rollout { 50.0 } } returns true
            }
            val flag2 by boolean<Context>(default = false) {
                rule { rollout { 50.0 } } returns true
            }
            val flag3 by boolean<Context>(default = false) {
                rule { rollout { 50.0 } } returns true
            }
        }

        val executor = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(5000)
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        // Stress test: 5000 evaluations across 50 threads
        repeat(5000) { i ->
            executor.submit {
                try {
                    val context = Context(
                        locale = AppLocale.EN_US,
                        platform = Platform.WEB,
                        appVersion = Version(1, 0, 0),
                        stableId = StableId.of(String.format("%032d", i))
                    )

                    // Evaluate all flags to maximize digest usage
                    TestFeatures.flag1.evaluate(context)
                    TestFeatures.flag2.evaluate(context)
                    TestFeatures.flag3.evaluate(context)
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()

        if (errors.isNotEmpty()) {
            println("CRITICAL: SHA-256 digest is not thread-safe!")
            errors.forEach { it.printStackTrace() }
        }

        assertTrue(
            errors.isEmpty(),
            "Concurrent digest usage failed - MessageDigest may not be thread-safe"
        )

        /*
         * EXPECTED FINDING:
         * If shaDigestSpi is a shared singleton, this test WILL FAIL
         * MessageDigest is explicitly documented as NOT thread-safe
         * Each thread needs its own instance or synchronization is required
         */
    }

    // ============================================
    // ATTACK 4: Namespace Registry Concurrent Access
    // ============================================

    @Test
    fun `ATTACK - concurrent registration in same namespace`() {
        /*
         * ATTACK: Multiple containers in same namespace accessed concurrently
         * RESULT: Test namespace registry thread-safety
         * DANGER: Registry might not handle concurrent writes correctly
         */

        // Create multiple containers in same namespace
        val containers = (1..10).map { i ->
            object : FeatureContainer<Namespace.Global>(Namespace.Global) {
                val feature by boolean<Context>(default = i % 2 == 0)
            }
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        // Access all containers concurrently
        containers.forEach { container ->
            executor.submit {
                try {
                    container.feature // Trigger registration
                    container.allFeatures()
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue(errors.isEmpty(), "Concurrent namespace access caused errors: $errors")
    }

    // ============================================
    // ATTACK 5: Memory Visibility - No Synchronization
    // ============================================
    @Volatile
    var readerSawFeature = false

    @Test
    fun `ATTACK - memory visibility of lazy initialization without volatile`() {
        /*
         * ATTACK: Test if changes are visible across threads
         * RESULT: Without proper memory barriers, threads might see stale data
         * DANGER: Thread A registers feature, Thread B might not see it
         */

        val container = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val visibilityTest by boolean<Context>(default = true)
        }

        val writerThread = Thread {
            // Writer thread registers the feature
            container.visibilityTest
        }

        val readerThread = Thread {
            // Reader thread tries to see the registration
            Thread.sleep(100) // Give writer time
            readerSawFeature = container.allFeatures().isNotEmpty()
        }

        writerThread.start()
        readerThread.start()

        writerThread.join()
        readerThread.join()

        assertTrue(
            readerSawFeature,
            "Memory visibility issue: reader didn't see writer's registration"
        )

        /*
         * If this fails, it indicates lack of proper synchronization
         * in the lazy registration mechanism
         */
    }

    // ============================================
    // ATTACK 6: Rule List Modification During Iteration
    // ============================================

    @Test
    fun `ATTACK - modification during iteration of conditional values`() {
        /*
         * ATTACK: Try to trigger concurrent modification
         * RESULT: Test if rule list is properly immutable
         * DANGER: If rules can be modified during evaluation, could crash
         */

        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val manyRulesFlag by boolean<Context>(default = false) {
                // Create many rules to increase iteration time
                repeat(100) { i ->
                    rule {
                        note("rule-$i")
                        if (i % 3 == 0) platforms(Platform.ANDROID)
                        if (i % 3 == 1) platforms(Platform.IOS)
                        if (i % 3 == 2) platforms(Platform.WEB)
                        rollout { (i % 100).toDouble() }
                    } returns (i % 2 == 0)
                }
            }
        }

        val executor = Executors.newFixedThreadPool(20)
        val latch = CountDownLatch(500)
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        repeat(500) { i ->
            executor.submit {
                try {
                    val context = Context(
                        locale = AppLocale.EN_US,
                        platform = Platform.entries[i % Platform.entries.size],
                        appVersion = Version(1, 0, 0),
                        stableId = StableId.of(String.format("%032d", i))
                    )

                    TestFeatures.manyRulesFlag.evaluate(context)
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue(
            errors.isEmpty(),
            "Concurrent iteration over rules caused errors: $errors"
        )
    }

    // ============================================
    // ATTACK 7: Context Mutation During Evaluation
    // ============================================

    @Test
    fun `ATTACK - mutating context during evaluation if mutable implementation used`() {
        /*
         * ATTACK: Use mutable context and modify during evaluation
         * RESULT: Test if evaluation assumes immutable context
         * DANGER: Changing context mid-evaluation could break matching
         */

        // Create mutable context implementation (violates Context contract)
        data class MutableContext(
            override var locale: AppLocale,
            override var platform: Platform,
            override var appVersion: Version,
            override val stableId: StableId
        ) : Context

        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val contextDependentFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.ANDROID)
                } returns true

                rule {
                    platforms(Platform.IOS)
                } returns false
            }
        }

        val mutableContext = MutableContext(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Evaluate while mutating context from another thread
        val executor = Executors.newFixedThreadPool(2)
        val results = ConcurrentHashMap<Int, Boolean>()

        repeat(100) { i ->
            executor.submit {
                // Mutator thread
                mutableContext.platform = if (i % 2 == 0) Platform.ANDROID else Platform.IOS
            }

            executor.submit {
                // Evaluator thread
                val result = TestFeatures.contextDependentFlag.evaluate(mutableContext)
                results[i] = result
            }
        }

        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        /*
         * IMPACT:
         * If evaluation doesn't defensively copy context,
         * mutations during evaluation could cause:
         * 1. Inconsistent rule matching
         * 2. Wrong bucket calculation
         * 3. Non-deterministic results
         *
         * This violates the implicit immutability contract
         */
    }

    // ============================================
    // ATTACK 8: Stress Test AllFeatures Concurrent Iteration
    // ============================================

    @Test
    fun `ATTACK - concurrent calls to allFeatures() during registration`() {
        /*
         * ATTACK: Call allFeatures() while features are being registered
         * RESULT: Test if iteration is safe during modification
         * DANGER: ConcurrentModificationException or inconsistent results
         */

        val container = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val f1 by boolean<Context>(default = true)
            val f2 by boolean<Context>(default = true)
            val f3 by boolean<Context>(default = true)
            val f4 by boolean<Context>(default = true)
            val f5 by boolean<Context>(default = true)
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val errors = ConcurrentHashMap.newKeySet<Throwable>()
        val sizes = ConcurrentHashMap.newKeySet<Int>()

        repeat(100) { i ->
            executor.submit {
                try {
                    when (i % 6) {
                        0 -> container.f1 // Register features
                        1 -> container.f2
                        2 -> container.f3
                        3 -> container.f4
                        4 -> container.f5
                        5 -> sizes.add(container.allFeatures().size) // Iterate
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue(
            errors.isEmpty(),
            "Concurrent allFeatures() iteration caused errors: $errors"
        )

        println("Observed sizes during concurrent registration: $sizes")
        // Sizes might be 0,1,2,3,4,5 depending on timing
        assertTrue(sizes.max() <= 5, "Saw more features than expected")
    }

    // ============================================
    // FINDINGS SUMMARY
    // ============================================
    /*
     * CONCURRENCY VULNERABILITIES TESTED:
     *
     * 1. LAZY REGISTRATION RACE CONDITION
     *    - Multiple threads accessing unregistered features simultaneously
     *    - Tests if delegation is thread-safe
     *
     * 2. SHA-256 DIGEST THREAD-SAFETY (CRITICAL)
     *    - MessageDigest is NOT thread-safe by Java spec
     *    - If shaDigestSpi is shared, concurrent calls will corrupt data
     *    - This could cause wrong bucketing, security issues
     *
     * 3. NAMESPACE REGISTRY CONTENTION
     *    - Multiple containers modifying same namespace concurrently
     *    - Tests registry internal synchronization
     *
     * 4. MEMORY VISIBILITY
     *    - Tests if changes are visible across threads without proper barriers
     *    - Could cause stale reads
     *
     * 5. RULE LIST CONCURRENT MODIFICATION
     *    - Tests immutability of rule lists during evaluation
     *    - ConcurrentModificationException risk
     *
     * 6. CONTEXT MUTATION
     *    - Mutable context violates implicit contract
     *    - Could cause non-deterministic evaluation
     *
     * 7. ITERATION DURING MODIFICATION
     *    - allFeatures() called while registration in progress
     *    - Tests collection thread-safety
     *
     * CRITICAL FINDING:
     * The biggest risk is the shared MessageDigest instance.
     * Java documentation explicitly states MessageDigest is NOT thread-safe.
     * If the code uses a shared instance, concurrent evaluations will:
     * - Produce wrong hash values
     * - Assign users to wrong buckets
     * - Break determinism guarantees
     * - Potentially crash with ArrayIndexOutOfBoundsException
     *
     * RECOMMENDATION:
     * Use ThreadLocal<MessageDigest> or synchronize digest access
     */
}
