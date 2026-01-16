@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.state

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.values.FeatureId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class FlagStateServiceTest {
    @Test
    fun `updateFlag updates and returns flag`() {
        val snapshot = snapshotWithRules()
        val service = InMemoryFlagStateService(snapshot)
        val featureId = snapshot.flags.first().key

        val updated = service.updateFlag(featureId) { flag -> flag.copy(isActive = false) }

        assertNotNull(updated)
        assertEquals(false, updated?.isActive)
        assertEquals(false, service.getSnapshot().flags.first().isActive)
    }

    @Test
    fun `addRule appends new rule`() {
        val snapshot = snapshotWithoutRules()
        val service = InMemoryFlagStateService(snapshot)
        val featureId = snapshot.flags.first().key
        val rule = SerializableRule(value = FlagValue.BooleanValue(true))

        val updatedSnapshot = service.addRule(featureId, rule)

        assertEquals(1, updatedSnapshot.flags.first().rules.size)
    }

    @Test
    fun `updateRule replaces rule at index`() {
        val snapshot = snapshotWithRules()
        val service = InMemoryFlagStateService(snapshot)
        val featureId = snapshot.flags.first().key

        service.updateRule(featureId, 0) { rule -> rule.copy(note = "Updated") }

        val updatedNote = service.getSnapshot().flags.first().rules.first().note
        assertEquals("Updated", updatedNote)
    }

    @Test
    fun `deleteRule removes rule at index`() {
        val snapshot = snapshotWithMultipleRules()
        val service = InMemoryFlagStateService(snapshot)
        val featureId = snapshot.flags.first().key

        val updatedSnapshot = service.deleteRule(featureId, 0)

        assertEquals(1, updatedSnapshot.flags.first().rules.size)
        assertEquals("Second", updatedSnapshot.flags.first().rules.first().note)
    }

    @Test
    fun `createDefaultRule uses provided value and defaults`() {
        val rule = createDefaultRule(FlagValue.StringValue("stripe"))

        assertEquals(100.0, rule.rampUp)
        assertEquals("New rule", rule.note)
        assertEquals("stripe", (rule.value as FlagValue.StringValue).value)
    }

    private fun snapshotWithoutRules(): SerializableSnapshot =
        SerializableSnapshot(
            flags =
                listOf(
                    SerializableFlag(
                        key = FeatureId.create("ui", "dark_mode"),
                        defaultValue = FlagValue.BooleanValue(false),
                    ),
                ),
        )

    private fun snapshotWithRules(): SerializableSnapshot =
        SerializableSnapshot(
            flags =
                listOf(
                    SerializableFlag(
                        key = FeatureId.create("ui", "dark_mode"),
                        defaultValue = FlagValue.BooleanValue(false),
                        rules =
                            listOf(
                                SerializableRule(
                                    value = FlagValue.BooleanValue(true),
                                    note = "Initial",
                                ),
                            ),
                    ),
                ),
        )

    private fun snapshotWithMultipleRules(): SerializableSnapshot =
        SerializableSnapshot(
            flags =
                listOf(
                    SerializableFlag(
                        key = FeatureId.create("ui", "dark_mode"),
                        defaultValue = FlagValue.BooleanValue(false),
                        rules =
                            listOf(
                                SerializableRule(
                                    value = FlagValue.BooleanValue(true),
                                    note = "First",
                                ),
                                SerializableRule(
                                    value = FlagValue.BooleanValue(false),
                                    note = "Second",
                                ),
                            ),
                    ),
                ),
        )
}
