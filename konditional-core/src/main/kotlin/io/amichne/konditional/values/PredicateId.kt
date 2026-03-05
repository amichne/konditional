package io.amichne.konditional.values

import io.amichne.konditional.core.features.Identifiable.Named.NonBlank
import java.nio.charset.StandardCharsets
import java.util.UUID

@JvmInline
value class PredicateId(override val value: String) : NonBlank {
    init { validate() }

    override fun toString(): String = value

    companion object {
        internal fun forRuleInlinePredicate(ruleId: RuleId, ordinal: Int): PredicateId = fromSeed(
            "konditional:predicate:inline-rule:${ruleId.value}:${ordinal.coerceAtLeast(0)}",
        )

        private fun fromSeed(seed: String): PredicateId = PredicateId(
            UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8)).toString(),
        )
    }
}
