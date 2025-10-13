package io.amichne.konditional.core

/**
 * StableId represents a stable identifier for a user or device.
 *
 * This is typically a unique identifier that is unlikely to change over time.
 *
 * @property hexId The normalized, hexadecimal representation of the stable identifier.
 * @property id The string representation of the stable identifier.
 *
 * @constructor Create empty Stable id
 */
sealed interface StableId {
    val id: String

    companion object {
        fun of(id: String): StableId = Factory.Instance(HexId(id))
    }

    private object Factory {
        data class Instance(val hexId: HexId) : StableId {
            override val id: String
                get() = hexId.id
        }
    }
}
