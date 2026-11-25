package io.amichne.konditional.context.fact

/**
 * A reusable policy: given a vocabulary [S], it builds one portable [FactExpression].
 *
 * [S] states what the condition needs. It can be a whole [FactSchema] or an interface that a
 * schema implements, such as `HasTemperature`. A condition that needs less fits any schema that
 * supplies more, because the receiver is contravariant.
 *
 * A rule binds its condition once, when the flag is declared, and keeps the expression.
 * Evaluation, explanation, and snapshot export never run the lambda again.
 */
typealias Condition<S> = S.() -> FactExpression

/** Binds every condition to the same receiver, in order, and requires all of them. */
fun <S> allOf(first: Condition<S>, vararg rest: Condition<S>): Condition<S> = allOf(listOf(first) + rest)

/** Binds every condition to the same receiver, in order. An empty collection is a declaration error. */
fun <S> allOf(conditions: Collection<Condition<S>>): Condition<S> {
    val children = nonEmptyConditions("allOf", conditions)
    return { allOf(children.map { it(this) }) }
}

/** Binds every condition to the same receiver, in order, and requires at least one of them. */
fun <S> anyOf(first: Condition<S>, vararg rest: Condition<S>): Condition<S> = anyOf(listOf(first) + rest)

/** Binds every condition to the same receiver, in order. An empty collection is a declaration error. */
fun <S> anyOf(conditions: Collection<Condition<S>>): Condition<S> {
    val children = nonEmptyConditions("anyOf", conditions)
    return { anyOf(children.map { it(this) }) }
}

/** Negates the bound expression. See [not] on expressions for the absent-fact rule. */
fun <S> not(condition: Condition<S>): Condition<S> = { not(condition()) }

/** Gives the bound expression a domain name. A blank label is a declaration error. */
fun <S> named(label: String, condition: Condition<S>): Condition<S> {
    require(label.isNotBlank()) { "A condition label must not be blank" }
    return { named(label, condition()) }
}

private fun <S> nonEmptyConditions(operator: String, conditions: Collection<Condition<S>>): List<Condition<S>> {
    require(conditions.isNotEmpty()) { "$operator needs at least one condition" }
    return immutableListCopy(conditions)
}
