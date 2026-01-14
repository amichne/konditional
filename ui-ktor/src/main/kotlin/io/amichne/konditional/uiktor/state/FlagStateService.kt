@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.state

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.values.FeatureId
import java.util.concurrent.atomic.AtomicReference

interface FlagStateService {
    fun getSnapshot(): SerializableSnapshot

    fun updateFlag(
        flagKey: FeatureId,
        updater: (SerializableFlag) -> SerializableFlag,
    ): SerializableFlag?

    fun addRule(
        flagKey: FeatureId,
        rule: SerializableRule,
    ): SerializableSnapshot

    fun updateRule(
        flagKey: FeatureId,
        ruleIndex: Int,
        updater: (SerializableRule) -> SerializableRule,
    ): SerializableSnapshot

    fun deleteRule(
        flagKey: FeatureId,
        ruleIndex: Int,
    ): SerializableSnapshot
}

class InMemoryFlagStateService(
    initialSnapshot: SerializableSnapshot,
) : FlagStateService {
    private val state = AtomicReference(initialSnapshot)

    override fun getSnapshot(): SerializableSnapshot = state.get()

    override fun updateFlag(
        flagKey: FeatureId,
        updater: (SerializableFlag) -> SerializableFlag,
    ): SerializableFlag? =
        state
            .updateAndGet { snapshot ->
                val flags = snapshot.flags.map { flag ->
                    if (flag.key == flagKey) {
                        updater(flag)
                    } else {
                        flag
                    }
                }
                snapshot.copy(flags = flags)
            }
            .flags
            .find { it.key == flagKey }

    override fun addRule(
        flagKey: FeatureId,
        rule: SerializableRule,
    ): SerializableSnapshot =
        state.updateAndGet { snapshot ->
            val flags = snapshot.flags.map { flag ->
                if (flag.key == flagKey) {
                    flag.copy(rules = flag.rules + rule)
                } else {
                    flag
                }
            }
            snapshot.copy(flags = flags)
        }

    override fun updateRule(
        flagKey: FeatureId,
        ruleIndex: Int,
        updater: (SerializableRule) -> SerializableRule,
    ): SerializableSnapshot =
        state.updateAndGet { snapshot ->
            val flags = snapshot.flags.map { flag ->
                if (flag.key == flagKey && ruleIndex in flag.rules.indices) {
                    val updatedRules = flag.rules.mapIndexed { index, rule ->
                        if (index == ruleIndex) updater(rule) else rule
                    }
                    flag.copy(rules = updatedRules)
                } else {
                    flag
                }
            }
            snapshot.copy(flags = flags)
        }

    override fun deleteRule(
        flagKey: FeatureId,
        ruleIndex: Int,
    ): SerializableSnapshot =
        state.updateAndGet { snapshot ->
            val flags = snapshot.flags.map { flag ->
                if (flag.key == flagKey) {
                    val updatedRules = flag.rules.filterIndexed { index, _ -> index != ruleIndex }
                    flag.copy(rules = updatedRules)
                } else {
                    flag
                }
            }
            snapshot.copy(flags = flags)
        }
}

fun createDefaultRule(defaultValue: FlagValue<*>): SerializableRule =
    SerializableRule(
        value = defaultValue,
        rampUp = 100.0,
        note = "New rule",
        locales = emptySet(),
        platforms = emptySet(),
        axes = emptyMap(),
    )
