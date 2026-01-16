@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.html

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.summary
import kotlinx.html.unsafe

fun FlowContent.renderFlagEditor(
    flag: SerializableFlag,
    basePath: String = "/config",
) {
    div {
        classes = setOf("min-h-screen", "bg-background")

        div {
            classes = setOf("max-w-4xl", "mx-auto", "p-6", "space-y-6")

            renderFlagEditorHeader(flag, basePath)
            renderFlagEditorTabs(flag, basePath)
        }
    }
}

private fun FlowContent.renderFlagEditorHeader(
    flag: SerializableFlag,
    basePath: String,
) {
    val flagKey = extractFlagKey(flag.key)

    div {
        classes = setOf("flex", "items-center", "gap-4")

        button {
            classes = buttonClasses(variant = ButtonVariant.GHOST, size = ButtonSize.ICON)
            attributes["hx-get"] = basePath
            attributes["hx-target"] = "#main-content"
            attributes["hx-swap"] = "innerHTML"
            attributes["hx-push-url"] = "true"

            unsafe {
                raw(
                    """<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>""",
                )
            }
        }

        div {
            classes = setOf("flex-1")
            div {
                classes = setOf("flex", "items-center", "gap-2")
                h2 {
                    classes = setOf("text-xl", "font-semibold", "font-mono")
                    +flagKey
                }
                renderValueTypeBadge(flag.defaultValue)
            }
            p {
                classes = setOf("text-sm", "text-muted-foreground", "font-mono")
                +flag.key.toString()
            }
        }

        div {
            classes = setOf("flex", "items-center", "gap-2")
            span {
                classes = setOf("text-sm", "text-muted-foreground")
                +"Active"
            }
            label {
                classes = setOf("relative", "inline-block")
                input(type = InputType.checkBox) {
                    classes = switchClasses()
                    checked = flag.isActive
                    attributes["data-state"] = if (flag.isActive) "checked" else "unchecked"
                    attributes["hx-post"] = "$basePath/flag/${flag.key}/toggle"
                    attributes["hx-target"] = "#main-content"
                    attributes["hx-swap"] = "innerHTML"
                }
                span {
                    classes =
                        setOf(
                            "pointer-events-none",
                            "block",
                            "h-5",
                            "w-5",
                            "rounded-full",
                            "bg-background",
                            "shadow-lg",
                            "ring-0",
                            "transition-transform",
                            "data-[state=checked]:translate-x-5",
                            "data-[state=unchecked]:translate-x-0",
                        )
                    attributes["data-state"] = if (flag.isActive) "checked" else "unchecked"
                }
            }
        }
    }
}

private fun FlowContent.renderFlagEditorTabs(
    flag: SerializableFlag,
    basePath: String,
) {
    div {
        classes = setOf("space-y-4")

        div {
            classes =
                setOf(
                    "inline-flex",
                    "h-10",
                    "items-center",
                    "justify-center",
                    "rounded-md",
                    "bg-muted",
                    "p-1",
                    "text-muted-foreground",
                )

            button {
                classes =
                    setOf(
                        "inline-flex",
                        "items-center",
                        "justify-center",
                        "gap-2",
                        "whitespace-nowrap",
                        "rounded-sm",
                        "px-3",
                        "py-1.5",
                        "text-sm",
                        "font-medium",
                        "ring-offset-background",
                        "transition-all",
                        "bg-background",
                        "text-foreground",
                        "shadow-sm",
                    )
                unsafe {
                    raw(
                        """<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>""",
                    )
                }
                +"Configure"
            }

            button {
                classes =
                    setOf(
                        "inline-flex",
                        "items-center",
                        "justify-center",
                        "gap-2",
                        "whitespace-nowrap",
                        "rounded-sm",
                        "px-3",
                        "py-1.5",
                        "text-sm",
                        "font-medium",
                        "ring-offset-background",
                        "transition-all",
                        "hover:bg-muted",
                        "hover:text-foreground",
                    )
                attributes["hx-get"] = "$basePath/flag/${flag.key}/json"
                attributes["hx-target"] = "#tab-content"
                attributes["hx-swap"] = "innerHTML"

                unsafe {
                    raw(
                        """<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/>
                    </svg>""",
                    )
                }
                +"JSON"
            }
        }

        div {
            id = "tab-content"
            renderConfigTabContent(flag, basePath)
        }
    }
}

private fun FlowContent.renderConfigTabContent(
    flag: SerializableFlag,
    basePath: String,
) {
    div {
        classes = setOf("space-y-6", "mt-6")

        div {
            classes = cardClasses(elevation = 0) + setOf("overflow-hidden")
            div {
                classes = setOf("p-6", "pb-4")
                h4 {
                    classes = setOf("text-base", "font-semibold")
                    +"Default Value"
                }
            }
            div {
                classes = setOf("px-6", "pb-6")
                renderValueEditor(flag.defaultValue, "${flag.key}/default", basePath)
            }
        }

        renderRulesSection(flag, basePath)
    }
}

internal fun FlowContent.renderRulesSection(
    flag: SerializableFlag,
    basePath: String,
) {
    div {
        classes = setOf("space-y-4")

        div {
            classes = setOf("flex", "items-center", "justify-between")
            h3 {
                classes = setOf("font-semibold")
                +"Targeting Rules"
            }
            button {
                classes = buttonClasses(variant = ButtonVariant.OUTLINE, size = ButtonSize.SM)
                attributes["hx-post"] = "$basePath/flag/${flag.key}/rule"
                attributes["hx-target"] = "#rules-list"
                attributes["hx-swap"] = "beforeend"

                unsafe {
                    raw(
                        """<svg class="h-4 w-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                    </svg>""",
                    )
                }
                +"Add Rule"
            }
        }

        div {
            id = "rules-list"
            classes = setOf("space-y-3")
            renderRulesList(flag, basePath)
        }
    }
}

internal fun FlowContent.renderRulesList(
    flag: SerializableFlag,
    basePath: String,
) {
    if (flag.rules.isEmpty()) {
        div {
            classes = cardClasses(elevation = 0) + setOf("border-dashed")
            div {
                classes = setOf("py-8", "text-center", "text-muted-foreground")
                p { +"No rules defined. The default value will be used for all users." }
                button {
                    classes = buttonClasses(variant = ButtonVariant.LINK) + "mt-2"
                    attributes["hx-post"] = "$basePath/flag/${flag.key}/rule"
                    attributes["hx-target"] = "#rules-list"
                    attributes["hx-swap"] = "innerHTML"

                    +"Add your first rule"
                }
            }
        }
    } else {
        flag.rules.forEachIndexed { index, rule ->
            renderRuleCard(flag, rule, index, basePath)
        }
    }
}

internal fun FlowContent.renderRuleCard(
    flag: SerializableFlag,
    rule: SerializableRule,
    index: Int,
    basePath: String,
) {
    details {
        classes =
            cardClasses(elevation = 0) +
                setOf(
                    "transition-all",
                    "[&[open]]:ring-2",
                    "[&[open]]:ring-primary",
                )

        summary {
            classes =
                setOf(
                    "pb-2",
                    "px-6",
                    "pt-6",
                    "cursor-pointer",
                    "list-none",
                    "flex",
                    "items-center",
                    "justify-between",
                )

            div {
                classes = setOf("flex", "items-center", "gap-3")
                span {
                    classes = badgeClasses(BadgeVariant.OUTLINE)
                    +"Rule ${index + 1}"
                }
                span {
                    classes = setOf("text-sm", "text-muted-foreground")
                    +(rule.note ?: "No description")
                }
            }

            div {
                classes = setOf("flex", "items-center", "gap-2")
                renderValueDisplay(rule.value)
                span {
                    val variantClass =
                        if (rule.rampUp == 100.0) {
                            BadgeVariant.DEFAULT
                        } else {
                            BadgeVariant.SECONDARY
                        }
                    classes = badgeClasses(variantClass)
                    +"${rule.rampUp.toInt()}%"
                }
            }
        }

        div {
            classes = setOf("px-6", "pb-6", "pt-4", "border-t", "space-y-4")
            renderRuleEditor(rule, index, flag.key.toString(), basePath)
        }
    }
}

private fun FlowContent.renderValueDisplay(
    value: FlagValue<*>,
) {
    val displayText =
        when (value) {
            is FlagValue.BooleanValue -> value.value.toString()
            is FlagValue.StringValue -> value.value
            is FlagValue.IntValue -> value.value.toString()
            is FlagValue.DoubleValue -> value.value.toString()
            else -> "..."
        }

    span {
        classes =
            setOf(
                "text-sm",
                "font-mono",
                "px-2",
                "py-0.5",
                "rounded",
                "bg-muted",
                "text-muted-foreground",
            )
        +displayText
    }
}

internal fun FlowContent.renderValueEditor(
    value: FlagValue<*>,
    path: String,
    basePath: String,
) {
    when (value) {
        is FlagValue.BooleanValue -> {
            label {
                classes = setOf("relative", "inline-block")
                input(type = InputType.checkBox) {
                    classes = switchClasses()
                    checked = value.value
                    attributes["data-state"] = if (value.value) "checked" else "unchecked"
                    attributes["hx-post"] = "$basePath/$path"
                    attributes["hx-target"] = "#tab-content"
                }
                span {
                    classes =
                        setOf(
                            "pointer-events-none",
                            "block",
                            "h-5",
                            "w-5",
                            "rounded-full",
                            "bg-background",
                            "shadow-lg",
                            "transition-transform",
                            "data-[state=checked]:translate-x-5",
                            "data-[state=unchecked]:translate-x-0",
                        )
                    attributes["data-state"] = if (value.value) "checked" else "unchecked"
                }
            }
        }

        is FlagValue.StringValue -> {
            input(type = InputType.text) {
                classes = inputClasses()
                this.value = value.value
                attributes["hx-post"] = "$basePath/$path"
                attributes["hx-trigger"] = "change"
                attributes["hx-target"] = "#tab-content"
            }
        }

        is FlagValue.IntValue -> {
            input(type = InputType.number) {
                classes = inputClasses()
                this.value = value.value.toString()
                attributes["hx-post"] = "$basePath/$path"
                attributes["hx-trigger"] = "change"
                attributes["hx-target"] = "#tab-content"
            }
        }

        is FlagValue.DoubleValue -> {
            input(type = InputType.number) {
                classes = inputClasses()
                this.value = value.value.toString()
                step = "0.01"
                attributes["hx-post"] = "$basePath/$path"
                attributes["hx-trigger"] = "change"
                attributes["hx-target"] = "#tab-content"
            }
        }

        else -> {
            p {
                classes = setOf("text-sm", "text-muted-foreground")
                +"Value editor for ${value::class.simpleName} coming soon..."
            }
        }
    }
}
