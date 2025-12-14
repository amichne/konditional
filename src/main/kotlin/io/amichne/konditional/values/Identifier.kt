package io.amichne.konditional.values

import com.squareup.moshi.ToJson

@JvmInline
value class Identifier private constructor(private val string: String) : Comparable<Identifier> {
    constructor(
        vararg components: String,
    ) : this(components.joinToString(separator = SEPARATOR, prefix = PREFIX))

    override fun compareTo(other: Identifier): Int = string.compareTo(other.string)

    @ToJson
    override fun toString(): String = string
}

private const val PREFIX = "id::"
private const val SEPARATOR = "::"
