package io.amichne.konditional.values

import io.amichne.konditional.values.IdentifierEncoding.SEPARATOR

@JvmInline
value class FeatureId private constructor(
    override val plainId: String,
) : KonditionalId<FeatureId> {
    override val prefix: String
        get() = PREFIX

    constructor(
        namespaceSeed: String,
        key: String,
    ) : this(IdentifierEncoding.encode(prefix = PREFIX, components = listOf(namespaceSeed, key)))

    init {
        val parts = IdentifierEncoding.split(plainId)
        require(parts.size == EXPECTED_PARTS) {
            "FeatureId must be encoded as '$PREFIX$SEPARATOR<namespaceSeed>$SEPARATOR<key>': '$plainId'"
        }
        require(parts[0] == PREFIX) { "FeatureId prefix must be '$PREFIX': '$plainId'" }
        require(parts[1].isNotBlank()) { "FeatureId namespaceSeed must not be blank: '$plainId'" }
        require(parts[2].isNotBlank()) { "FeatureId key must not be blank: '$plainId'" }
    }

    override fun compareTo(other: FeatureId): Int = plainId.compareTo(other.plainId)

    override fun toString(): String = plainId

    companion object {
        const val PREFIX: String = "feature"
        const val LEGACY_PREFIX: String = "value"

        private const val EXPECTED_PARTS: Int = 3

        /**
         * Parses a serialized identifier into a canonical [FeatureId].
         *
         * Supports the legacy `value::...` prefix for backwards compatibility with older snapshots.
         */
        fun parse(plainId: String): FeatureId =
            IdentifierEncoding.split(plainId).let { (prefix: String, namespaceSeed: String, key: String) ->
                require(listOf(prefix, namespaceSeed, key).none { it.isEmpty() }) {
                    "FeatureId must be encoded as '<prefix>$SEPARATOR<namespaceSeed>$SEPARATOR<key>': '$plainId'"
                }
                require(prefix == PREFIX) { "FeatureId prefix must be '$PREFIX': '$plainId'" }
                return FeatureId(namespaceSeed = namespaceSeed, key = key)
            }
    }
}
