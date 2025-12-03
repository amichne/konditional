package io.amichne.konditional.core.id

import org.jetbrains.annotations.ApiStatus
import java.security.MessageDigest

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

    /**
     * Discrete bucketing value between 0.0 and 100.0 for the given [flagDefinition].
     *
     * Functionally this allows the caller to get the exact bucket value for this stable ID and flag,
     * which can then be compared against rollout percentages to determine inclusion.
     *
     * Uses the flag's feature key and salt to compute a stable hash-based bucket, same as used in rollouts.
     *
     * This is an experimental API and may change in future releases.
     *
     * @param flagDefinition
     * @return
     */
    @ApiStatus.Experimental
    fun discrete(
        key: String,
        salt: String,
    ): Double =
        discrete(key, hexId, salt) / 100.0

    companion object {
        /**
         * The sole creator for [StableId]. Requires a valid string identifier.
         *
         * @param id The string representation of the stable identifier.
         * @return A [StableId] instance with the provided identifier.
         *
         * @throws IllegalArgumentException if the provided id is not a valid hexadecimal string.
         */
        fun of(id: String): StableId = Factory.Instance(HexId(id.lowercase()))

        internal fun discrete(
            flagKey: String,
            id: HexId,
            salt: String,
        ): Int = stableBucket(flagKey, id, salt)

        /**
         * Create a new MessageDigest instance per call to ensure thread-safety
         * MessageDigest is NOT thread-safe and cannot be shared across concurrent evaluations
         */
        private fun stableBucket(
            flagKey: String,
            id: HexId,
            salt: String,
        ): Int {
            val digest = MessageDigest.getInstance("SHA-256")
            return with(digest.digest("$salt:$flagKey:${id.id}".toByteArray(Charsets.UTF_8))) {
                (
                    (
                        get(0).toInt() and 0xFF shl 24 or
                            (get(1).toInt() and 0xFF shl 16) or
                            (get(2).toInt() and 0xFF shl 8) or
                            (get(3).toInt() and 0xFF)
                    ).toLong() and 0xFFFF_FFFFL
                ).mod(10_000L).toInt()
            }
        }
    }

    private object Factory {
        data class Instance(override val hexId: HexId) : StableId {
            override val id: String
                get() = hexId.id
        }
    }
}
