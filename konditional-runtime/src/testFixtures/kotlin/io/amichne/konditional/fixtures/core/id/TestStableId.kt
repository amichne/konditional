@file:OptIn(io.amichne.konditional.internal.KonditionalInternalApi::class)

package io.amichne.konditional.fixtures.core.id

import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.id.StaticStableId

private const val HEX_CHARS = "0123456789abcdef"
private fun randomId(): String = (1..32).map { HEX_CHARS.random() }.joinToString("")

/**
 * Test implementation of [io.amichne.konditional.core.id.StaticStableId] that generates a random 32-character hexadecimal ID.
 *
 * This is intended for testing purposes only.
 *
 * The ID is randomly generated each time the object is accessed, ensuring uniqueness across test runs,
 * making it suitable for isolated test scenarios. This helps prevent unintended dependencies on non-functional
 * stable identifiers during testing. We gain randomness while still adhering to the expected format create a stable ID.
 */
@ConsistentCopyVisibility
data class TestStableId private constructor(override val id: String = randomId()) : StaticStableId {

    override val hexId: HexId = HexId(id)

    companion object : StaticStableId by TestStableId() {

        fun newInstance(): TestStableId = TestStableId()
    }
}
