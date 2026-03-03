@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.runtime

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.ops.RegistryHooks
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Periodically fetches and loads configuration snapshots into a namespace registry.
 *
 * Connects a [SnapshotFetcher] to [NamespaceSnapshotLoader]. Fetch and parse
 * failures are logged but never propagate — the last successful configuration
 * remains active. Registry mutations are always atomic.
 *
 * @param namespace the namespace whose registry will be updated on each refresh
 * @param fetcher transport-level supplier of raw JSON snapshot strings
 * @param interval time between successive refreshes
 * @param options controls unknown-key and missing-flag handling during decode
 * @param hooks observability hooks; defaults to hooks already on the namespace registry
 * @param dispatcher coroutine dispatcher for fetch and load; defaults to [Dispatchers.IO]
 */
class NamespaceRefreshScheduler<M : Namespace>(
    private val namespace: M,
    private val fetcher: SnapshotFetcher,
    private val interval: Duration,
    private val options: SnapshotLoadOptions = SnapshotLoadOptions.fillMissingDeclaredFlags(),
    private val hooks: RegistryHooks = namespace.registry.hooks,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val loader = NamespaceSnapshotLoader.forNamespace(namespace)
    private val activeJob = AtomicReference<Job?>(null)

    val isRunning: Boolean get() = activeJob.get()?.isActive == true

    /**
     * Starts periodic refresh. The first refresh fires immediately; subsequent
     * refreshes fire after each [interval] elapses.
     *
     * @throws IllegalStateException if already running
     * @return this instance for fluent chaining
     */
    fun start(): NamespaceRefreshScheduler<M> {
        check(!isRunning) { "Scheduler for namespace '${namespace.id}' is already running" }
        activeJob.set(
            CoroutineScope(SupervisorJob() + dispatcher).launch {
                while (isActive) {
                    refreshNow()
                    delay(interval.toMillis())
                }
            },
        )
        return this
    }

    /** Cancels the active refresh loop. Safe to call when already stopped. */
    fun stop() {
        activeJob.getAndSet(null)?.cancel()
    }

    /**
     * Triggers a single fetch-decode-load cycle immediately, independent of
     * the periodic loop.
     *
     * Returns [Result.success] with the loaded [Configuration] on success, or
     * [Result.failure] if either the fetch or the parse step fails. A failure
     * never modifies the active registry.
     */
    suspend fun refreshNow(): Result<Configuration> {
        hooks.logger.debug { "[konditional] Fetching snapshot for namespace '${namespace.id}'" }
        return fetcher.fetch().fold(
            onSuccess = { json ->
                loader.load(json, options).also { result ->
                    result.onFailure { error ->
                        hooks.logger.warn(
                            { "[konditional] Parse failed for namespace '${namespace.id}': ${error.message}" },
                            error,
                        )
                    }
                }
            },
            onFailure = { error ->
                hooks.logger.warn(
                    { "[konditional] Fetch failed for namespace '${namespace.id}': ${error.message}" },
                    error,
                )
                Result.failure(error)
            },
        )
    }

    companion object {
        @Suppress("LongParameterList")
        fun <M : Namespace> forNamespace(
            namespace: M,
            fetcher: SnapshotFetcher,
            interval: Duration,
            options: SnapshotLoadOptions = SnapshotLoadOptions.fillMissingDeclaredFlags(),
            hooks: RegistryHooks = namespace.registry.hooks,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): NamespaceRefreshScheduler<M> = NamespaceRefreshScheduler(
            namespace, fetcher, interval, options, hooks, dispatcher,
        )
    }
}
