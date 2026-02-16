package io.amichne.konditional.httpserver

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileBackedSnapshotStorageTest {
    @Test
    fun `stores entries in deterministic key order`() {
        val dir = Files.createTempDirectory("konditional-http")
        val storage = FileBackedSnapshotStorage(dir.resolve("snapshots.json"))

        storage.put("zeta", "{\"enabled\":false}")
        storage.put("alpha", "{\"enabled\":true}")

        assertEquals(listOf("alpha", "zeta"), storage.all().keys.toList())
    }

    @Test
    fun `reloads persisted snapshots from disk`() {
        val dir = Files.createTempDirectory("konditional-http")
        val path = dir.resolve("snapshots.json")
        val first = FileBackedSnapshotStorage(path)

        first.put("mobile", "{\"variant\":\"A\"}")

        val second = FileBackedSnapshotStorage(path)
        assertEquals("{\"variant\":\"A\"}", second.get("mobile"))
    }

    @Test
    fun `delete returns false when namespace is missing`() {
        val dir = Files.createTempDirectory("konditional-http")
        val storage = FileBackedSnapshotStorage(dir.resolve("snapshots.json"))

        assertFalse(storage.delete("missing"))
        storage.put("existing", "{}")
        assertTrue(storage.delete("existing"))
    }
}
