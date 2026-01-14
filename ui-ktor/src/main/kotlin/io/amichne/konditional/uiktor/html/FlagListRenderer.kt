@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.html

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.ValueType
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.values.FeatureId
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h3
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.unsafe

data class FlagsByNamespace(
    val namespace: String,
    val flags: List<SerializableFlag>,
)

fun groupFlagsByNamespace(snapshot: SerializableSnapshot): List<FlagsByNamespace> =
    snapshot.flags
        .groupBy { flag -> extractNamespace(flag.key) }
        .map { (namespace, flags) -> FlagsByNamespace(namespace, flags) }
        .sortedBy(FlagsByNamespace::namespace)

fun FlowContent.renderFlagListPage(
    snapshot: SerializableSnapshot,
    basePath: String = "/config",
) {
    val flagsByNamespace = groupFlagsByNamespace(snapshot)

    div {
        classes = setOf("min-h-screen", "bg-background")

        div {
            classes = setOf("max-w-6xl", "mx-auto", "p-6")

            div {
                classes = setOf("mb-8")
                h1 {
                    classes = setOf("text-3xl", "font-bold", "mb-2")
                    +"Feature Flags"
                }
                p {
                    classes = setOf("text-muted-foreground")
                    +"Configure feature flags with type-safe editors. Click a flag to edit."
                }
            }

            if (flagsByNamespace.isEmpty()) {
                div {
                    classes =
                        setOf(
                            "text-center",
                            "py-12",
                            "border-2",
                            "border-dashed",
                            "border-border",
                            "rounded-lg",
                            "bg-muted/20",
                        )
                    p {
                        classes = setOf("text-muted-foreground")
                        +"No feature flags configured"
                    }
                }
            } else {
                div {
                    classes = setOf("grid", "gap-6", "md:grid-cols-2", "lg:grid-cols-3")

                    flagsByNamespace.forEach { (namespace, flags) ->
                        renderNamespaceSection(namespace, flags, basePath)
                    }
                }
            }
        }
    }
}

private fun FlowContent.renderNamespaceSection(
    namespace: String,
    flags: List<SerializableFlag>,
    basePath: String,
) {
    div {
        h3 {
            classes =
                setOf(
                    "text-sm",
                    "font-semibold",
                    "text-muted-foreground",
                    "uppercase",
                    "tracking-wider",
                    "mb-3",
                )
            +namespace
        }
        div {
            classes = setOf("space-y-2")
            flags.forEach { flag ->
                renderFlagCard(flag, basePath)
            }
        }
    }
}

private fun FlowContent.renderFlagCard(
    flag: SerializableFlag,
    basePath: String,
) {
    val flagKey = extractFlagKey(flag.key)

    button {
        classes =
            cardClasses(elevation = 1, interactive = true) +
                setOf(
                    "w-full",
                    "text-left",
                    "p-4",
                    "animate-fade-in",
                )

        attributes["hx-get"] = "$basePath/flag/${flag.key}"
        attributes["hx-target"] = "#main-content"
        attributes["hx-swap"] = "innerHTML"
        attributes["hx-push-url"] = "true"

        div {
            classes = setOf("flex", "items-start", "justify-between", "gap-3")

            div {
                classes = setOf("flex-1", "min-w-0")

                div {
                    classes = setOf("flex", "items-center", "gap-2", "mb-1")
                    span {
                        classes = setOf("font-mono", "text-sm", "font-medium", "truncate")
                        +flagKey
                    }
                    if (!flag.isActive) {
                        span {
                            classes = badgeClasses(BadgeVariant.SECONDARY) + "text-xs"
                            +"Inactive"
                        }
                    }
                }

                div {
                    classes = setOf("flex", "items-center", "gap-2")
                    renderValueTypeBadge(flag.defaultValue)
                    if (flag.rules.isNotEmpty()) {
                        span {
                            classes = setOf("text-xs", "text-muted-foreground")
                            +"${flag.rules.size} rule${if (flag.rules.size != 1) "s" else ""}"
                        }
                    }
                }
            }

            span {
                classes = setOf("text-muted-foreground", "shrink-0", "mt-1")
                unsafe {
                    raw(
                        """<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                    </svg>""",
                    )
                }
            }
        }
    }
}

internal fun FlowContent.renderValueTypeBadge(value: FlagValue<*>) {
    val (bgClass, textClass, label) =
        when (value.toValueType()) {
            ValueType.BOOLEAN -> Triple("bg-success/10", "text-success", "Boolean")
            ValueType.STRING -> Triple("bg-info/10", "text-info", "String")
            ValueType.INT -> Triple("bg-warning/10", "text-warning", "Int")
            ValueType.LONG -> Triple("bg-warning/10", "text-warning", "Long")
            ValueType.DOUBLE -> Triple("bg-warning/10", "text-warning", "Double")
            ValueType.ENUM -> Triple("bg-accent/10", "text-accent", "Enum")
            ValueType.DATA_CLASS -> Triple("bg-muted", "text-muted-foreground", "Data Class")
            ValueType.JSON -> Triple("bg-muted", "text-muted-foreground", "Json")
            ValueType.JSON_OBJECT -> Triple("bg-muted", "text-muted-foreground", "Json Object")
            ValueType.JSON_ARRAY -> Triple("bg-muted", "text-muted-foreground", "Json Array")
        }

    span {
        classes =
            setOf(
                "inline-flex",
                "items-center",
                "gap-1.5",
                "px-2",
                "py-0.5",
                "rounded-md",
                "text-xs",
                "font-semibold",
                "border",
                bgClass,
                textClass,
            )
        +label
    }
}

internal fun extractFlagKey(flagKey: FeatureId): String =
    flagKey.plainId
        .split("::")
        .let { parts ->
            if (parts.size >= 3) {
                parts[2]
            } else {
                flagKey.toString()
            }
        }

private fun extractNamespace(flagKey: FeatureId): String =
    flagKey.plainId
        .split("::")
        .let { parts ->
            if (parts.size >= 3) {
                parts[1]
            } else {
                "default"
            }
        }
