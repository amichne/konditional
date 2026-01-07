package io.amichne.konditional.serialization.instance

import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.fixtures.CommonTestFeatures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigurationPatchTest {
    @Test
    fun emptyPatchHasNoFlagsOrRemoveKeys() {
        val patch = ConfigurationPatch.empty()
        assertTrue(patch.flags.isEmpty())
        assertTrue(patch.removeKeys.isEmpty())
    }

    @Test
    fun patchBuilderAddsFlagsCorrectly() {
        val flag = FlagDefinition(
            feature = CommonTestFeatures.testFeature,
            bounds = emptyList(),
            defaultValue = false,
        )
        val patch = ConfigurationPatch.patch {
            add(flag)
        }
        assertEquals(1, patch.flags.size)
        assertTrue(patch.flags.containsKey(CommonTestFeatures.testFeature))
        assertEquals(flag, patch.flags[CommonTestFeatures.testFeature])
    }

    @Test
    fun patchBuilderRemovesFlagsCorrectly() {
        val patch = ConfigurationPatch.patch {
            remove(CommonTestFeatures.testFeature)
        }
        assertTrue(patch.removeKeys.contains(CommonTestFeatures.testFeature))
        assertTrue(patch.flags.isEmpty())
    }

    @Test
    fun patchBuilderHandlesAddAndRemoveForSameFeature() {
        val flag = FlagDefinition(
            feature = CommonTestFeatures.testFeature,
            bounds = emptyList(),
            defaultValue = false,
        )
        val patch = ConfigurationPatch.patch {
            add(flag)
            remove(CommonTestFeatures.testFeature)
        }
        assertTrue(patch.flags.isEmpty())
        assertTrue(patch.removeKeys.contains(CommonTestFeatures.testFeature))
    }

    @Test
    fun applyPatchAddsAndRemovesFlagsCorrectly() {
        val flag1 = FlagDefinition(
            feature = CommonTestFeatures.testFeature,
            bounds = emptyList(),
            defaultValue = false,
        )
        val flag2 = FlagDefinition(
            feature = CommonTestFeatures.enabledFeature,
            bounds = emptyList(),
            defaultValue = true,
        )
        val initialConfig = Configuration(mapOf(CommonTestFeatures.testFeature to flag1))
        val patch = ConfigurationPatch(
            flags = mapOf(CommonTestFeatures.enabledFeature to flag2),
            removeKeys = setOf(CommonTestFeatures.testFeature),
        )
        val updatedConfig = patch.applyTo(initialConfig)
        assertFalse(updatedConfig.flags.containsKey(CommonTestFeatures.testFeature))
        assertTrue(updatedConfig.flags.containsKey(CommonTestFeatures.enabledFeature))
        assertEquals(flag2, updatedConfig.flags[CommonTestFeatures.enabledFeature])
    }
}
