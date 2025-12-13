package io.amichne.konditional.core.id

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
    val hexId: HexId

    companion object {
        private data class Instance(override val hexId: HexId) : StableId {
            override val id: String
                get() = hexId.id
        }

        /**
         * Creates a StableId from an arbitrary string by hex-encoding its bytes.
         *
         * This is useful when migrating from systems whose identifiers are not hex-encoded.
         * The mapping is deterministic and stable across processes.
         *
         * @param id The string representation of the stable identifier.
         * @return A [StableId] instance with the provided identifier.
         */
        fun of(input: String): StableId = require(input.isNotBlank()) { "StableId input must not be blank" }
            .run {
                Instance(
                    HexId(input.lowercase().encodeToByteArray().joinToString(separator = "") { "%02x".format(it) })
                )
            }
    }
}
