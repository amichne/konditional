file=konditional-http-server/src/test/kotlin/io/amichne/konditional/httpserver/FileBackedSnapshotStorageTest.kt
package=io.amichne.konditional.httpserver
imports=java.nio.file.Files,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.httpserver.FileBackedSnapshotStorageTest|kind=class|decl=class FileBackedSnapshotStorageTest
methods:
- fun `stores entries in deterministic key order`()
- fun `reloads persisted snapshots from disk`()
- fun `delete returns false when namespace is missing`()
