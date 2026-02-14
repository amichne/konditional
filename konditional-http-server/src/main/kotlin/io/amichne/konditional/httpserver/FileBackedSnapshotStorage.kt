package io.amichne.konditional.httpserver

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicReference

/**
 * Persists namespace snapshots as a deterministic map in a local JSON file.
 *
 * Invariants:
 * - readers always observe a complete immutable snapshot map;
 * - writes are atomic for in-process readers via [AtomicReference];
 * - filesystem writes use atomic move to prevent partially written files.
 */
public class FileBackedSnapshotStorage(
    private val storagePath: Path,
    private val jsonAdapter: JsonAdapter<Map<String, String>> = defaultAdapter(),
) {
    private val state: AtomicReference<Map<String, String>> = AtomicReference(loadFromDisk())

    /** Returns a snapshot payload for [namespace], or `null` if it is not stored. */
    public fun get(namespace: String): String? = state.get()[namespace]

    /** Returns all namespace snapshots in stable key ordering. */
    public fun all(): Map<String, String> = state.get()

    /**
     * Stores [snapshotJson] for [namespace], replacing any previous value.
     *
     * The update is linearizable: readers observe either the old map or the new map.
     */
    public fun put(namespace: String, snapshotJson: String) {
        updateState { current ->
            current + (namespace to snapshotJson)
        }
    }

    /** Removes a namespace snapshot. Returns true when a value existed and was removed. */
    public fun delete(namespace: String): Boolean {
        var removed = false
        updateState { current ->
            if (namespace !in current) {
                current
            } else {
                removed = true
                current - namespace
            }
        }
        return removed
    }

    private fun updateState(transform: (Map<String, String>) -> Map<String, String>) {
        while (true) {
            val previous = state.get()
            val next = transform(previous).toSortedMap()
            if (previous == next) return
            if (state.compareAndSet(previous, next)) {
                persist(next)
                return
            }
        }
    }

    private fun loadFromDisk(): Map<String, String> {
        if (Files.notExists(storagePath)) return emptyMap()
        val content = Files.readString(storagePath, StandardCharsets.UTF_8)
        if (content.isBlank()) return emptyMap()
        return jsonAdapter.fromJson(content)?.toSortedMap().orEmpty()
    }

    private fun persist(payload: Map<String, String>) {
        Files.createDirectories(storagePath.parent ?: Path.of("."))
        val tmp = storagePath.resolveSibling("${storagePath.fileName}.tmp")
        Files.writeString(tmp, jsonAdapter.toJson(payload), StandardCharsets.UTF_8)
        Files.move(
            tmp,
            storagePath,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private companion object {
        private fun defaultAdapter(): JsonAdapter<Map<String, String>> {
            val mapType = Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                String::class.java,
            )
            return Moshi.Builder().build().adapter(mapType)
        }
    }
}
