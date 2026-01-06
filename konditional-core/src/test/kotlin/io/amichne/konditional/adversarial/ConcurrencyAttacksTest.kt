package io.amichne.konditional.adversarial

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
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
    fun `ATTACK - concurrent access to eagerly registered features`() {
        /*
         * ATTACK: Hammer feature access concurrently.
         * EXPECTATION: Features are registered at container initialization (t0),
         *              so concurrent reads should be safe and should not trigger any registration races.
         */

        val container = object : Namespace.TestNamespaceFacade("concurrency-access") {
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

        // Verify all features were registered at t0
        assertEquals(3, container.allFeatures().size)
    }

    // ============================================
    // ATTACK 2: Concurrent Evaluation Under Load
    // ============================================

    @Test
    fun `ATTACK - concurrent flag evaluation with different contexts`() {
        /*
         * ATTACK: Hammer flag evaluation with many threads
         * RESULT: TestNamespace thread-safety create evaluation logic
         * DANGER: Race conditions in:
         *         - Rule matching
         *         - Bucket calculation (SHA-256 digest)
         *         - Value retrieval
         */

        val TestNamespaceFeatures = object : Namespace.TestNamespaceFacade("concurrent-flag-eval") {
            val highContentionFlag by boolean<Context>(default = false) {
                rule(true) {
                    platforms(Platform.ANDROID)
                    rampUp { 50.0 }
                }

                rule(true) {
                    platforms(Platform.IOS)
                    rampUp { 30.0 }
                }
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
                        locale = AppLocale.UNITED_STATES,
                        platform = if (i % 2 == 0) Platform.ANDROID else Platform.IOS,
                        appVersion = Version(1, 0, 0),
                        stableId = StableId.of(String.format("%032d", i))
                    )

                    val result = TestNamespaceFeatures.highContentionFlag.evaluate(context)
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

        // Verify determinism: same contextFn always gives same result
        val context1 = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of(String.format("%032d", 0))
        )

        val result1 = TestNamespaceFeatures.highContentionFlag.evaluate(context1)
        val result2 = TestNamespaceFeatures.highContentionFlag.evaluate(context1)
        assertEquals(result1, result2, "Non-deterministic evaluation detected")
    }

    // ============================================
    // ATTACK 3: SHA-256 MessageDigest Thread Safety
    // ============================================

    @Test
    fun `ATTACK - concurrent SHA-256 digest usage in bucketing`() {
        /*
         * ATTACK: TestNamespace if MessageDigest.getInstance is thread-safe
         * RESULT: MessageDigest is NOT thread-safe if shared
         * DANGER: If FlagDefinition.shaDigestSpi is shared across threads,
         *         concurrent digest() calls will corrupt each other
         *
         * NOTE: MessageDigest.getInstance("SHA-256") returns a NEW instance,
         *       but the code has: "val shaDigestSpi: MessageDigest"
         *       If this is a singleton, it's a CRITICAL BUG
         */

        val TestNamespaceFeatures = object : Namespace.TestNamespaceFacade("concurrent-digest") {
            // Create many flags to stress digest usage
            val flag1 by boolean<Context>(default = false) {
                rule(true) { rampUp { 50.0 } }
            }
            val flag2 by boolean<Context>(default = false) {
                rule(true) { rampUp { 50.0 } }
            }
            val flag3 by boolean<Context>(default = false) {
                rule(true) { rampUp { 50.0 } }
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
                        locale = AppLocale.UNITED_STATES,
                        platform = Platform.ANDROID,
                        appVersion = Version(1, 0, 0),
                        stableId = StableId.of(String.format("%032d", i))
                    )

                    // Evaluate all flags to maximize digest usage
                    TestNamespaceFeatures.flag1.evaluate(context)
                    TestNamespaceFeatures.flag2.evaluate(context)
                    TestNamespaceFeatures.flag3.evaluate(context)
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
         * ATTACK: Multiple namespaces registering features concurrently
         * RESULT: Namespace registry thread-safety and global FeatureRegistry behavior
         * DANGER: Shared mutable state during registration might not be thread-safe
         */

        val namespaces = (1..10).map { i ->
            object : Namespace.TestNamespaceFacade("concurrent-registration-$i") {
                val feature by boolean<Context>(default = i % 2 == 0)
            }
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        namespaces.forEach { namespace ->
            executor.submit {
                try {
                    namespace.feature // Trigger registration
                    namespace.allFeatures()
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
    fun `ATTACK - memory visibility of eager initialization across threads`() {
        /*
         * ATTACK: Ensure container initialization publishes registered features across threads.
         * EXPECTATION: Registration happens at container initialization (t0), and is visible to readers.
         */

        val container = object : Namespace.TestNamespaceFacade("memory-visibility") {
            val visibilityTest by boolean<Context>(default = true)
        }

        val writerThread = Thread { /* no-op: registration happens at t0 */ }

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
         * If this fails, it indicates lack create proper publication/synchronization
         * around container initialization visibility.
         */
    }

    // ============================================
    // ATTACK 6: Rule List Modification During Iteration
    // ============================================

    @Test
    fun `ATTACK - modification during iteration of conditional values`() {
        /*
         * ATTACK: Try to trigger concurrent modification
         * RESULT: TestNamespace if rule list is properly immutable
         * DANGER: If rules can be modified during evaluation, could crash
         */

        val TestNamespaceFeatures = object : Namespace.TestNamespaceFacade("concurrent-iteration") {
            val manyRulesFlag by boolean<Context>(default = false) {
                // Create many rules to increase iteration time
                repeat(100) { i ->
                    rule(i % 2 == 0) {
                        note("rule-$i")
                        if (i % 2 == 0) android()
                        if (i % 2 != 0) ios()
                        rampUp { (i % 100).toDouble() }
                    }
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
                        locale = AppLocale.UNITED_STATES,
                        platform = Platform.entries[i % Platform.entries.size],
                        appVersion = Version(1, 0, 0),
                        stableId = StableId.of(String.format("%032d", i))
                    )

                    TestNamespaceFeatures.manyRulesFlag.evaluate(context)
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
         * ATTACK: Use mutable contextFn and modify during evaluation
         * RESULT: TestNamespace if evaluation assumes immutable contextFn
         * DANGER: Changing contextFn mid-evaluation could break matching
         */

        // Create mutable contextFn implementation (violates Context contract)
        data class MutableContext(
            override var locale: AppLocale,
            override var platform: Platform,
            override var appVersion: Version,
            override val stableId: StableId,
        ) : Context

        val TestNamespaceFeatures = object : Namespace.TestNamespaceFacade("mutable-context") {
            val contextDependentFlag by boolean<Context>(default = false) {
                rule(true) {
                    platforms(Platform.ANDROID)
                }

                rule(false) {
                    platforms(Platform.IOS)
                }
            }
        }

        val mutableContext = MutableContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Evaluate while mutating contextFn from another thread
        val executor = Executors.newFixedThreadPool(2)
        val results = ConcurrentHashMap<Int, Boolean>()

        repeat(100) { i ->
            executor.submit {
                // Mutator thread
                mutableContext.platform = if (i % 2 == 0) Platform.ANDROID else Platform.IOS
            }

            executor.submit {
                // Evaluator thread
                val result = TestNamespaceFeatures.contextDependentFlag.evaluate(mutableContext)
                results[i] = result
            }
        }

        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        /*
         * IMPACT:
         * If evaluation doesn't defensively copy contextFn,
         * mutations during evaluation could cause:
         * 1. Inconsistent rule matching
         * 2. Wrong bucket calculation
         * 3. Non-deterministic results
         *
         * This violates the implicit immutability contract
         */
    }

    // ============================================
    // ATTACK 8: Stress TestNamespace AllFeatures Concurrent Iteration
    // ============================================

    @Test
    fun `ATTACK - concurrent calls to allFeatures() during registration`() {
        /*
         * ATTACK: Call allFeatures() concurrently with feature reads.
         * EXPECTATION: Features are registered at container initialization (t0),
         *              so allFeatures() is read-only and should be safe to call concurrently.
         */

        val container = object : Namespace.TestNamespaceFacade("concurrent-all-features") {
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
        assertTrue(sizes.max() <= 5, "Saw more features than expected")
        assertTrue(sizes.min() >= 5, "Saw fewer features than expected")
    }

    // ============================================
    // FINDINGS SUMMARY
    // ============================================
    /*
     * CONCURRENCY VULNERABILITIES TESTED:
     *
     * 1. FEATURE ACCESS CONCURRENCY
     *    - Multiple threads accessing features and allFeatures() concurrently
     *    - Ensures eager registration avoids registration races
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
     *    - Tests immutability create rule lists during evaluation
     *    - ConcurrentModificationException risk
     *
     * 6. CONTEXT MUTATION
     *    - Mutable contextFn violates implicit contract
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
