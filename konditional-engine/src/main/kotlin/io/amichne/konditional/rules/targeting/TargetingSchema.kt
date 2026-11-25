package io.amichne.konditional.rules.targeting

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult

/** A portable operand type. Its canonical text is part of the snapshot contract. */
class TargetingType<T : Any> @PublishedApi internal constructor(
    internal val format: (T) -> String,
    private val parse: (String) -> T?,
) {
    internal fun parseCanonical(text: String): T? = parse(text)?.takeIf { format(it) == text }

    internal fun literal(value: T): String = format(value).also {
        requireNotNull(parseCanonical(it)) { "Operand codec produced a noncanonical literal" }
    }

    companion object {
        /** A case-sensitive string operand. */
        val string: TargetingType<String> = TargetingType({ it }, { it })

        /** A signed 32-bit integer expressed in decimal. */
        val int: TargetingType<Int> = TargetingType(Int::toString, String::toIntOrNull)

        /** A lowercase true or false operand. */
        val boolean: TargetingType<Boolean> = TargetingType(Boolean::toString, String::toBooleanStrictOrNull)

        /**
         * Defines a stable literal for an application-owned operand, including a structured argument.
         * [decode] must return an independent immutable value or null for malformed input.
         * Both functions must be deterministic. The snapshot reader accepts only canonical literals.
         */
        fun <T : Any> custom(encode: (T) -> String, decode: (String) -> T?): TargetingType<T> =
            TargetingType(encode, decode)

        /** Uses the enum constant's declared name as its stable snapshot representation. */
        inline fun <reified E : Enum<E>> enum(): TargetingType<E> {
            val byName = enumValues<E>().associateBy { it.name }
            return TargetingType({ it.name }, byName::get)
        }
    }
}

/** A declaration that a flag can expose to snapshots through its [TargetingSchema]. */
sealed class TargetingDeclaration<in C : Context>(val key: String) {
    init {
        require(key.isNotBlank()) { "A targeting declaration needs a nonblank key" }
    }
}

/** A typed custom field of the evaluation context. [read] must be deterministic. */
class ContextAttribute<C : Context, T : Any>(
    key: String,
    private val type: TargetingType<T>,
    private val read: (C) -> T,
) : TargetingDeclaration<C>(key) {
    /** Matches when the evaluation context supplies one of these typed values. */
    fun isOneOf(first: T, vararg others: T): TargetingExpression<C> =
        TargetingExpression(bind((listOf(first) + others).map(type::literal)))

    internal fun resolve(operands: List<String>): ParseResult<Targeting<C>> =
        if (operands.isEmpty() || operands.any { type.parseCanonical(it) == null }) {
            invalid("Attribute '$key' needs one or more valid operands")
        } else {
            ParseResult.Success(bind(operands))
        }
    private fun bind(operands: List<String>): Targeting<C> {
        val accepted = operands.toSet()
        return Targeting.Attribute(this, java.util.List.copyOf(accepted)) { type.format(read(it)) in accepted }
    }
}

/**
 * A locally implemented condition selectable by its stable [key] and positive [version].
 * [matches] must be deterministic for a fixed context and argument. Register a new version
 * when its semantics change. The snapshot carries the argument, key, and version.
 */
class NamedPredicate<C : Context, A : Any>(
    key: String,
    val version: Int,
    private val argumentType: TargetingType<A>,
    private val matches: C.(A) -> Boolean,
) : TargetingDeclaration<C>(key) {
    init {
        require(version > 0) { "Predicate '$key' needs a positive version" }
    }

    /** Constructs a typed invocation with an immutable argument. */
    operator fun invoke(argument: A): TargetingExpression<C> {
        val text = argumentType.format(argument)
        val decoded = requireNotNull(argumentType.parseCanonical(text)) { "Predicate '$key' has an invalid argument" }
        return TargetingExpression(bind(text, decoded))
    }

    internal fun resolve(argument: String): ParseResult<Targeting<C>> =
        argumentType.parseCanonical(argument)?.let { ParseResult.Success(bind(argument, it)) }
            ?: invalid("Invalid argument for predicate '$key' version $version")

    private fun bind(text: String, argument: A): Targeting<C> =
        Targeting.Predicate(this, text) { context -> context.matches(argument) }
}

/**
 * Immutable vocabulary for snapshot rules on a feature with evaluation context [C].
 * Register each attribute key and each predicate key/version once. Duplicate declarations
 * fail at schema construction. Reuse a schema on flags that share the same vocabulary.
 */
class TargetingSchema<in C : Context>(vararg declarations: TargetingDeclaration<C>) {
    private val attributes = declarations.filterIsInstance<ContextAttribute<C, *>>()
        .uniqueBy { it.key }
    private val predicates = declarations.filterIsInstance<NamedPredicate<C, *>>()
        .uniqueBy { it.key to it.version }

    internal fun resolveAttribute(key: String, operands: List<String>): ParseResult<Targeting<C>> =
        attributes[key]?.resolve(operands) ?: invalid("Unknown context attribute '$key'")

    internal fun resolvePredicate(key: String, version: Int, argument: String): ParseResult<Targeting<C>> =
        predicates[key to version]?.resolve(argument) ?: invalid("Unknown predicate '$key' version $version")

    internal fun checkDeclarations(node: Targeting<*>) {
        when (node) {
            is Targeting.Attribute<*> -> require(attributes[node.key] === node.declaration) {
                "Attribute '${node.key}' does not belong to the feature's targeting schema"
            }
            is Targeting.Predicate<*> -> require(predicates[node.key to node.version] === node.declaration) {
                "Predicate '${node.key}' version ${node.version} does not belong to the feature's targeting schema"
            }
            is Targeting.All<*> -> node.targets.forEach(::checkDeclarations)
            is Targeting.AnyOf<*> -> node.targets.forEach(::checkDeclarations)
            is Targeting.Guarded<*, *> -> checkDeclarations(node.inner)
            is Targeting.Axis, is Targeting.Custom<*>, is Targeting.Locale,
            is Targeting.Platform, is Targeting.Version -> Unit
        }
    }

    internal companion object {
        val empty = TargetingSchema<Context>()
    }
}

private fun <T, K> List<T>.uniqueBy(keyOf: (T) -> K): Map<K, T> = buildMap {
    for (value in this@uniqueBy) {
        val key = keyOf(value)
        require(put(key, value) == null) { "Duplicate targeting declaration '$key'" }
    }
}

private fun invalid(reason: String): ParseResult.Failure = ParseResult.Failure(ParseError.invalidSnapshot(reason))
