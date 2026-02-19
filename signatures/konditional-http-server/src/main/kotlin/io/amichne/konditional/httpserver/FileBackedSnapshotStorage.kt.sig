file=konditional-http-server/src/main/kotlin/io/amichne/konditional/httpserver/FileBackedSnapshotStorage.kt
package=io.amichne.konditional.httpserver
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.Moshi,com.squareup.moshi.Types,java.nio.charset.StandardCharsets,java.nio.file.Files,java.nio.file.Path,java.nio.file.StandardCopyOption,java.util.concurrent.atomic.AtomicReference
type=io.amichne.konditional.httpserver.FileBackedSnapshotStorage|kind=class|decl=public class FileBackedSnapshotStorage( private val storagePath: Path, private val jsonAdapter: JsonAdapter<Map<String, String>> = defaultAdapter(), )
fields:
- private val state: AtomicReference<Map<String, String>>
methods:
- public fun get(namespace: String): String?
- public fun all(): Map<String, String>
- public fun put(namespace: String, snapshotJson: String)
- public fun delete(namespace: String): Boolean
- private fun updateState(transform: (Map<String, String>) -> Map<String, String>)
- private fun loadFromDisk(): Map<String, String>
- private fun persist(payload: Map<String, String>)
