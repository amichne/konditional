@file:OptIn(KonditionalInternalApi::class, ExperimentalCoroutinesApi::class)

package io.amichne.konditional.runtime

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NamespaceRefreshSchedulerTest {

    // Each call produces a fresh namespace with an isolated registry to prevent cross-test pollution.
    private fun makeNamespace(): Namespace.TestNamespaceFacade =
        object : Namespace.TestNamespaceFacade("refresh-test-${UUID.randomUUID()}") {
            @Suppress("unused")
            val flag by boolean<Context>(default = false)
        }

    @Test
    fun `refreshNow returns success and updates registry when fetch succeeds`() = runTest {
        val namespace = makeNamespace()
        val validJson = namespace.dump()
        val fetcher = SnapshotFetcher { Result.success(validJson) }
        val scheduler = NamespaceRefreshScheduler(namespace, fetcher, Duration.ofMinutes(1))

        val result = scheduler.refreshNow()

        assertTrue(result.isSuccess)
        assertTrue(namespace.configuration.flags.isNotEmpty())
    }

    @Test
    fun `refreshNow returns failure and leaves registry unchanged when fetch fails`() = runTest {
        val namespace = makeNamespace()
        val initialFlags = namespace.configuration.flags.toMap()
        val fetcher = SnapshotFetcher { Result.failure(RuntimeException("network error")) }
        val scheduler = NamespaceRefreshScheduler(namespace, fetcher, Duration.ofMinutes(1))

        val result = scheduler.refreshNow()

        assertTrue(result.isFailure)
        assertEquals(initialFlags, namespace.configuration.flags)
    }

    @Test
    fun `refreshNow returns failure and leaves registry unchanged when JSON is invalid`() = runTest {
        val namespace = makeNamespace()
        val initialFlags = namespace.configuration.flags.toMap()
        val fetcher = SnapshotFetcher { Result.success("not valid json at all") }
        val scheduler = NamespaceRefreshScheduler(namespace, fetcher, Duration.ofMinutes(1))

        val result = scheduler.refreshNow()

        assertTrue(result.isFailure)
        assertEquals(initialFlags, namespace.configuration.flags)
    }

    @Test
    fun `start fires immediately then repeats at the configured interval`() = runTest {
        val namespace = makeNamespace()
        val validJson = namespace.dump()
        var fetchCount = 0
        val fetcher = SnapshotFetcher { fetchCount++; Result.success(validJson) }
        val scheduler = NamespaceRefreshScheduler(
            namespace = namespace,
            fetcher = fetcher,
            interval = Duration.ofSeconds(5),
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        scheduler.start()
        assertEquals(1, fetchCount, "first fetch must fire immediately on start")

        advanceTimeBy(5_001)
        assertEquals(2, fetchCount, "second fetch must fire after one interval")

        advanceTimeBy(5_001)
        assertEquals(3, fetchCount, "third fetch must fire after two intervals")

        scheduler.stop()
    }

    @Test
    fun `stop cancels the refresh loop and prevents further fetches`() = runTest {
        val namespace = makeNamespace()
        val validJson = namespace.dump()
        var fetchCount = 0
        val fetcher = SnapshotFetcher { fetchCount++; Result.success(validJson) }
        val scheduler = NamespaceRefreshScheduler(
            namespace = namespace,
            fetcher = fetcher,
            interval = Duration.ofSeconds(5),
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        scheduler.start()
        assertEquals(1, fetchCount)
        assertTrue(scheduler.isRunning)

        scheduler.stop()
        assertFalse(scheduler.isRunning)

        advanceTimeBy(15_000)
        assertEquals(1, fetchCount, "no additional fetches must occur after stop")
    }

    @Test
    fun `start throws IllegalStateException when scheduler is already running`() = runTest {
        val namespace = makeNamespace()
        val fetcher = SnapshotFetcher { Result.success(namespace.dump()) }
        val scheduler = NamespaceRefreshScheduler(
            namespace = namespace,
            fetcher = fetcher,
            interval = Duration.ofSeconds(5),
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        scheduler.start()
        val thrown = runCatching { scheduler.start() }

        assertIs<IllegalStateException>(thrown.exceptionOrNull())
        assertTrue(thrown.exceptionOrNull()!!.message!!.contains(namespace.id))

        scheduler.stop()
    }

    @Test
    fun `failed refresh does not clobber last known good configuration`() = runTest {
        val namespace = makeNamespace()
        val validJson = namespace.dump()
        var shouldFail = false
        val fetcher = SnapshotFetcher {
            if (shouldFail) {
                Result.failure(RuntimeException("transient error"))
            } else {
                Result.success(validJson)
            }
        }
        val scheduler = NamespaceRefreshScheduler(
            namespace = namespace,
            fetcher = fetcher,
            interval = Duration.ofMinutes(1),
            options = SnapshotLoadOptions.fillMissingDeclaredFlags(),
        )

        val goodResult = scheduler.refreshNow()
        assertTrue(goodResult.isSuccess)
        val configAfterGoodLoad = namespace.configuration

        shouldFail = true
        val failResult = scheduler.refreshNow()
        assertTrue(failResult.isFailure)

        assertEquals(
            configAfterGoodLoad,
            namespace.configuration,
            "registry must retain the last successful configuration after a fetch failure",
        )
    }

    @Test
    fun `scheduler can be restarted after being stopped`() = runTest {
        val namespace = makeNamespace()
        val validJson = namespace.dump()
        var fetchCount = 0
        val fetcher = SnapshotFetcher { fetchCount++; Result.success(validJson) }
        val scheduler = NamespaceRefreshScheduler(
            namespace = namespace,
            fetcher = fetcher,
            interval = Duration.ofSeconds(5),
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        scheduler.start()
        assertEquals(1, fetchCount)

        scheduler.stop()
        assertFalse(scheduler.isRunning)

        scheduler.start()
        assertEquals(2, fetchCount, "restart must trigger an immediate fetch")
        assertTrue(scheduler.isRunning)

        scheduler.stop()
    }
}
