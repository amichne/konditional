@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.internal

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.rules.targeting.TargetingSchema
import io.amichne.konditional.rules.targeting.isStandardGuard
import io.amichne.konditional.rules.versions.VersionRange

/** Portable targeting operators and their operands before binding to a feature's compiled vocabulary. */
@KonditionalInternalApi
sealed interface SerializedTargeting {
    data class AllOf(val operands: List<SerializedTargeting>) : SerializedTargeting
    data class AnyOf(val operands: List<SerializedTargeting>) : SerializedTargeting
    data class Locale(val ids: Set<String>) : SerializedTargeting
    data class Platform(val ids: Set<String>) : SerializedTargeting
    data class Version(val range: VersionRange) : SerializedTargeting
    data class Axis(val axisId: String, val allowedIds: Set<String>) : SerializedTargeting
    data class Attribute(val key: String, val operands: List<String>) : SerializedTargeting
    data class Predicate(val key: String, val version: Int, val argument: String) : SerializedTargeting
}

internal fun Targeting<*>.serialize(): SerializedTargeting =
    when (this) {
        is Targeting.All<*> -> SerializedTargeting.AllOf(targets.map { it.serialize() })
        is Targeting.AnyOf<*> -> SerializedTargeting.AnyOf(targets.map { it.serialize() })
        is Targeting.Locale -> SerializedTargeting.Locale(ids)
        is Targeting.Platform -> SerializedTargeting.Platform(ids)
        is Targeting.Version -> SerializedTargeting.Version(range)
        is Targeting.Axis -> SerializedTargeting.Axis(axisId, allowedIds)
        is Targeting.Attribute<*> -> SerializedTargeting.Attribute(key, operands)
        is Targeting.Predicate<*> -> SerializedTargeting.Predicate(key, version, argument)
        is Targeting.Guarded<*, *> -> if (isStandardGuard()) inner.serialize() else codeOnlyTargeting()
        is Targeting.Custom<*> -> codeOnlyTargeting()
    }

private fun codeOnlyTargeting(): Nothing =
    throw KonditionalBoundaryFailure(ParseError.invalidSnapshot(
        "Cannot export an inline targeting predicate or custom context guard. Declare a named predicate for snapshot targeting.",
    ))

internal fun <C : Context> SerializedTargeting.bindRule(schema: TargetingSchema<C>): ParseResult<Targeting.All<C>> =
    (if (this is SerializedTargeting.AllOf) operands else listOf(this)).bind(schema) { Targeting.All(it) }

private fun <C : Context> SerializedTargeting.bind(schema: TargetingSchema<C>): ParseResult<Targeting<C>> =
    when (this) {
        is SerializedTargeting.AllOf -> operands.bind(schema) { Targeting.All(it) }
        is SerializedTargeting.AnyOf -> operands.bind(schema) { Targeting.AnyOf(it) }
        is SerializedTargeting.Locale -> ParseResult.Success(Targeting.locale(ids.frozenIds()))
        is SerializedTargeting.Platform -> ParseResult.Success(Targeting.platform(ids.frozenIds()))
        is SerializedTargeting.Version -> ParseResult.Success(Targeting.version(range))
        is SerializedTargeting.Axis -> ParseResult.Success(Targeting.Axis(axisId, allowedIds.frozenIds()))
        is SerializedTargeting.Attribute -> schema.resolveAttribute(key, operands)
        is SerializedTargeting.Predicate -> schema.resolvePredicate(key, version, argument)
    }

private inline fun <C : Context, R> List<SerializedTargeting>.bind(
    schema: TargetingSchema<C>,
    combine: (List<Targeting<C>>) -> R,
): ParseResult<R> {
    val children = ArrayList<Targeting<C>>(size)
    for (operand in this) {
        when (val child = operand.bind(schema)) {
            is ParseResult.Success -> children.add(child.value)
            is ParseResult.Failure -> return child
        }
    }
    return ParseResult.Success(combine(java.util.List.copyOf(children)))
}

/** Preserve the wire order for diagnostics and isolate a compiled rule from transport-owned collections. */
internal fun Set<String>.frozenIds(): Set<String> = java.util.Collections.unmodifiableSet(LinkedHashSet(this))
