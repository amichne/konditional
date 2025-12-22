package io.amichne.konditional.configstate

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfigurationStateSupportedValuesCatalogTest {
    @Test
    fun `catalog covers all field types`() {
        val supported = ConfigurationStateSupportedValuesCatalog.current()
        assertEquals(FieldType.entries.map { it.name }.toSet(), supported.byType.keys)
    }

    @Test
    fun `bindings field types exist in byType`() {
        val supported = ConfigurationStateSupportedValuesCatalog.current()
        val fieldTypes = supported.bindings.values.map { it.name }.toSet()
        assertTrue(fieldTypes.all { it in supported.byType.keys })
    }

    @Test
    fun `locales and platforms are complete`() {
        val supported = ConfigurationStateSupportedValuesCatalog.current()

        val localeDescriptor = supported.byType.getValue(FieldType.LOCALES.name)
        assertIs<EnumOptionsDescriptor>(localeDescriptor)
        assertEquals(AppLocale.entries.map { it.id }.toSet(), localeDescriptor.options.map { it.value }.toSet())

        val platformDescriptor = supported.byType.getValue(FieldType.PLATFORMS.name)
        assertIs<EnumOptionsDescriptor>(platformDescriptor)
        assertEquals(Platform.entries.map { it.id }.toSet(), platformDescriptor.options.map { it.value }.toSet())
    }
}

