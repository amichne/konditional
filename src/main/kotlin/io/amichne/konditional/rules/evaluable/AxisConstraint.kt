package io.amichne.konditional.rules.evaluable

/**
 * Internal representation of an axis matching constraint within a rule.
 *
 * This class represents a constraint that requires a context's value along a specific
 * axis to be one of a set of allowed values. It's used internally by the rule evaluation
 * engine to determine if a context matches a rule's targeting criteria.
 *
 * ## Matching Semantics
 *
 * A context matches this constraint if:
 * - The context has a value for the axis (identified by [axisId])
 * - That value's ID is in the [allowedIds] set
 *
 * If the context doesn't have a value for this axis, the constraint does not match.
 *
 * ## Example
 *
 * ```kotlin
 * // Rule: target only PROD and STAGE environments
 * AxisConstraint(
 *     axisId = "environment",
 *     allowedIds = setOf("prod", "stage")
 * )
 * ```
 *
 * @property axisId The unique identifier of the axis being constrained
 * @property allowedIds The set of allowed value IDs for this axis
 */
internal data class AxisConstraint(
    val axisId: String,
    val allowedIds: Set<String>,
)
