package io.amichne.konditional.core.instance

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationDiffTest {
    @Test
    fun `diff reports added removed and changed flags`() {
        val ns = Namespace("diff-test")

        val features = object : FeatureContainer<Namespace>(ns) {
            val A by boolean<Context>(default = false)
            val B by boolean<Context>(default = false)
        }

        val before = ns.configuration
        val after = Configuration(
            flags = mapOf(
                features.A to FlagDefinition(
                    feature = features.A,
                    bounds = emptyList(),
                    defaultValue = true,
                )
            ),
            metadata = ConfigurationMetadata.of(version = "rev-2"),
        )

        val diff = before.diff(after)

        assertEquals("rev-2", diff.after.version)
        assertEquals(listOf(features.B.id), diff.removed.map { it.id })
        assertEquals(listOf(features.A.id), diff.changed.map { it.id })
        assertEquals(true, (diff.changed.single().after.defaultValue as ConfigValue.BooleanValue).value)
    }
}

