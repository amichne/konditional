package io.amichne.konditional.demo.ui.slices.tobebuilt

enum class ToBeBuiltSection(
    val label: String,
    val hrefPath: String,
    val fragmentPath: String,
) {
    INDEX(
        label = "Index",
        hrefPath = "/to-be-built",
        fragmentPath = "/to-be-built/fragments/index",
    ),
    PRIMITIVES_COLORS(
        label = "Primitives / Colors",
        hrefPath = "/to-be-built/primitives/colors",
        fragmentPath = "/to-be-built/fragments/primitives/colors",
    ),
    PRIMITIVES_TYPOGRAPHY(
        label = "Primitives / Typography",
        hrefPath = "/to-be-built/primitives/typography",
        fragmentPath = "/to-be-built/fragments/primitives/typography",
    ),
    COMPONENTS_INPUTS(
        label = "Components / Inputs",
        hrefPath = "/to-be-built/components/inputs",
        fragmentPath = "/to-be-built/fragments/components/inputs",
    ),
    PATTERNS_SCHEMA_FORMS(
        label = "Patterns / Schema Forms",
        hrefPath = "/to-be-built/patterns/schema-forms",
        fragmentPath = "/to-be-built/fragments/patterns/schema-forms",
    ),
}

data class ToBeBuiltNavItem(
    val section: ToBeBuiltSection,
)

data class ToBeBuiltShellModel(
    val title: String,
    val navItems: List<ToBeBuiltNavItem>,
    val initialSection: ToBeBuiltSection,
)
