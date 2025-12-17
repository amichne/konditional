package io.amichne.konditional.values

import com.squareup.moshi.FromJson
import io.amichne.konditional.values.IdentifierEncoding.SEPARATOR
import kotlin.reflect.KClass

/**
 * Canonically-encoded, strongly-typed identifiers.
 *
 * Identifier values are intended to be:
 * - Primary keys at storage boundaries (serialization, registries, diffs)
 * - Deterministic and comparable
 * - Resistant to accidental mixing across domains via distinct types
 *
 * Encoding convention:
 * `"<prefix>::<component1>::<component2>::..."`
 *
 * `prefix` is an explicit domain discriminator (e.g. `feature`), and components must be non-blank
 * and must not contain [IdentifierEncoding.SEPARATOR].
 */
interface KonditionalId<T : KonditionalId<T>> : Comparable<T> {
    val plainId: String

    override fun compareTo(other: T): Int = plainId.compareTo(other.plainId)

    interface Factory<T : KonditionalId<*>> {
        val kClass: KClass<T>
        val componentNames: List<String>

        val prefix: String
            get() = kClass.simpleName?.lowercase().toString()

        val builder: (String) -> KonditionalId<*>

        companion object {
            inline operator fun <reified T : KonditionalId<*>> invoke(
                componentNames: List<String>,
                crossinline builder: (String) -> KonditionalId<*>,
            ): Factory<T> = TFactory(T::class, componentNames) { builder(it) }
        }

        @FromJson
        fun parse(plainId: String): KonditionalId<*> {
            val idTypeName = kClass.simpleName?.lowercase().toString()
            val expectedParts = componentNames.size + 1
            val expectedEncoding =
                (listOf(element = prefix) + componentNames.map { "<$it>" }).joinToString(SEPARATOR)
            val parts = IdentifierEncoding.split(plainId = plainId)
            require(parts.size == expectedParts) { "$idTypeName must be encoded as '$expectedEncoding': '$plainId'" }
            val actualPrefix = parts[0]
            require(actualPrefix.isNotBlank()) { "$idTypeName prefix must not be blank: '$plainId'" }
            require(actualPrefix == prefix) { "$idTypeName prefix must be '$prefix': '$plainId'" }
            val components = parts.drop(1)
            componentNames.zip(components).forEach { (name, component) ->
                require(component.isNotBlank()) { "$idTypeName $name must not be blank: '$plainId'" }
            }
            return builder(components.joinToString { SEPARATOR })
        }

        fun of(
            components: List<String>,
        ): KonditionalId<*> {
            require(prefix.isNotBlank()) { "Identifier prefix must not be blank" }
            require(!prefix.contains(SEPARATOR)) { "Identifier prefix must not contain '${SEPARATOR}': '$prefix'" }
            components.forEachIndexed { index, component ->
                require(component.isNotBlank()) { "Identifier component[$index] must not be blank" }
                require(!component.contains(SEPARATOR)) {
                    "Identifier component[$index] must not contain '${SEPARATOR}': '$component'"
                }
            }
            return builder.invoke((listOf(prefix) + components).joinToString(SEPARATOR))
        }

        fun of(vararg string: String): KonditionalId<*> = of(string.toList())
    }
}

class TFactory<T : KonditionalId<*>>(
    override val kClass: KClass<T>,
    override val componentNames: List<String>,
    override val builder: (String) -> KonditionalId<*>
) : KonditionalId.Factory<T> {
}
