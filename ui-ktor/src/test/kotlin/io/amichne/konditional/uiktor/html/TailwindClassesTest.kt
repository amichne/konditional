package io.amichne.konditional.uiktor.html

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TailwindClassesTest {
    @Test
    fun `buttonClasses default variant`() {
        val classes = buttonClasses()
        assertTrue(classes.contains("bg-primary"))
        assertTrue(classes.contains("text-primary-foreground"))
        assertTrue(classes.contains("hover:bg-primary/90"))
    }

    @Test
    fun `buttonClasses outline variant`() {
        val classes = buttonClasses(variant = ButtonVariant.OUTLINE)
        assertTrue(classes.contains("border"))
        assertTrue(classes.contains("border-input"))
        assertTrue(classes.contains("bg-background"))
    }

    @Test
    fun `cardClasses with elevation`() {
        val classes = cardClasses(elevation = 1)
        assertTrue(classes.contains("rounded-lg"))
        assertTrue(classes.contains("border"))
        assertTrue(classes.contains("bg-card"))
        assertTrue(classes.contains("shadow-sm"))
    }

    @Test
    fun `cardClasses interactive`() {
        val classes = cardClasses(interactive = true)
        assertTrue(classes.contains("cursor-pointer"))
        assertTrue(classes.contains("transition-all"))
        assertTrue(classes.contains("hover:shadow-lg"))
    }

    @Test
    fun `badgeClasses default`() {
        val classes = badgeClasses()
        assertTrue(classes.contains("inline-flex"))
        assertTrue(classes.contains("rounded-md"))
        assertTrue(classes.contains("bg-primary"))
    }
}
