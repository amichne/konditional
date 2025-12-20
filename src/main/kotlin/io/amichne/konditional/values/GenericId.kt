package io.amichne.konditional.values

/**
 * Marker interface for stable identifiers that wrap string ids.
 */
interface GenericId {
    val id: String
}

interface LocaleTagId : GenericId {
    companion object {
        fun of(id: String): LocaleTagId = LocaleTagIdValue(id)
    }
}

@JvmInline
value class LocaleTagIdValue(override val id: String) : LocaleTagId {
    override fun toString(): String = id

    companion object {
        fun from(id: LocaleTagId): LocaleTagIdValue = LocaleTagIdValue(id.id)
    }
}

interface PlatformTagId : GenericId {
    companion object {
        fun of(id: String): PlatformTagId = PlatformTagIdValue(id)
    }
}

@JvmInline
value class PlatformTagIdValue(override val id: String) : PlatformTagId {
    override fun toString(): String = id

    companion object {
        fun from(id: PlatformTagId): PlatformTagIdValue = PlatformTagIdValue(id.id)
    }
}

interface AxisId : GenericId {
    companion object {
        fun of(id: String): AxisId = AxisIdValue(id)
    }
}

@JvmInline
value class AxisIdValue(override val id: String) : AxisId {
    override fun toString(): String = id

    companion object {
        fun from(id: AxisId): AxisIdValue = AxisIdValue(id.id)
    }
}

interface NamespaceId : GenericId {
    companion object {
        fun of(id: String): NamespaceId = NamespaceIdValue(id)
    }
}

@JvmInline
value class NamespaceIdValue(override val id: String) : NamespaceId {
    override fun toString(): String = id

    companion object {
        fun from(id: NamespaceId): NamespaceIdValue = NamespaceIdValue(id.id)
    }
}
