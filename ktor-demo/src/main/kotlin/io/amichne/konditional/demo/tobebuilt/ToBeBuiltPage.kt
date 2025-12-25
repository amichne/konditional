package io.amichne.konditional.demo.tobebuilt

internal sealed interface ToBeBuiltPage {
    val title: String
    val activeNavPrefix: String

    data object Index : ToBeBuiltPage {
        override val title: String = "To Be Built"
        override val activeNavPrefix: String = "/to-be-built/"
    }

    data object PrimitivesColors : ToBeBuiltPage {
        override val title: String = "Colors"
        override val activeNavPrefix: String = "/to-be-built/primitives"
    }

    data object PrimitivesTypography : ToBeBuiltPage {
        override val title: String = "Typography"
        override val activeNavPrefix: String = "/to-be-built/primitives"
    }

    data object ComponentsInputs : ToBeBuiltPage {
        override val title: String = "Inputs & Forms"
        override val activeNavPrefix: String = "/to-be-built/components"
    }

    data object PatternsSchemaForms : ToBeBuiltPage {
        override val title: String = "Schema-Driven Forms"
        override val activeNavPrefix: String = "/to-be-built/patterns"
    }

    data class Placeholder(
        override val title: String,
        override val activeNavPrefix: String,
        val description: String,
    ) : ToBeBuiltPage

    data class NotFound(val requestedPath: String) : ToBeBuiltPage {
        override val title: String = "Not Found"
        override val activeNavPrefix: String = ""
    }

    companion object {
        fun fromRequestPath(path: String): ToBeBuiltPage {
            val subPath = path.removePrefix("/to-be-built").trim('/').ifEmpty { "" }

            return when (subPath) {
                "" -> Index
                "primitives", "primitives/colors" -> PrimitivesColors
                "primitives/typography" -> PrimitivesTypography
                "primitives/spacing" -> Placeholder(
                    title = "Spacing",
                    activeNavPrefix = "/to-be-built/primitives",
                    description = "Spacing scale and layout density controls (to be built).",
                )
                "primitives/motion" -> Placeholder(
                    title = "Motion",
                    activeNavPrefix = "/to-be-built/primitives",
                    description = "Motion primitives and reduced-motion behavior (to be built).",
                )
                "components",
                "components/layout",
                "components/navigation",
                "components/overlays",
                "components/inputs",
                "components/data-display",
                    -> ComponentsInputs
                "patterns", "patterns/schema-forms" -> PatternsSchemaForms
                "patterns/safe-publishing" -> Placeholder(
                    title = "Safe Publishing",
                    activeNavPrefix = "/to-be-built/patterns",
                    description = "Draft → Review → Approve → Publish workflow UI (to be built).",
                )
                "patterns/large-datasets" -> Placeholder(
                    title = "Large Datasets",
                    activeNavPrefix = "/to-be-built/patterns",
                    description = "Virtualized tables, filtering, and pagination patterns (to be built).",
                )
                "playground" -> Placeholder(
                    title = "Playground",
                    activeNavPrefix = "/to-be-built/playground",
                    description = "Interactive theme/density playground (to be built).",
                )
                "demo" -> Placeholder(
                    title = "Config Demo",
                    activeNavPrefix = "/to-be-built/demo",
                    description = "Links to existing Konditional demo routes.",
                )
                else -> NotFound(requestedPath = path)
            }
        }
    }
}

