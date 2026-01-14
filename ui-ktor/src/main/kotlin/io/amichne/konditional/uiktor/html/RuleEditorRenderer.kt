@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor.html

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.SerializableRule
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h4
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.span
import kotlinx.html.unsafe

fun FlowContent.renderRuleEditor(
    rule: SerializableRule,
    ruleIndex: Int,
    flagKey: String,
    basePath: String,
) {
    div {
        classes = setOf("space-y-4")

        div {
            label {
                classes = setOf("text-sm", "font-medium", "leading-none", "mb-2", "block")
                htmlFor = "rule-note-$ruleIndex"
                +"Description"
            }
            input(type = InputType.text) {
                classes = inputClasses()
                id = "rule-note-$ruleIndex"
                name = "note"
                value = rule.note ?: ""
                placeholder = "e.g., Enable for beta users"
                attributes["hx-post"] = "$basePath/flag/$flagKey/rule/$ruleIndex/note"
                attributes["hx-trigger"] = "change"
                attributes["hx-target"] = "closest details"
            }
        }

        div {
            h4 {
                classes = setOf("text-sm", "font-medium", "mb-3")
                +"Value"
            }
            renderValueEditor(
                rule.value,
                "flag/$flagKey/rule/$ruleIndex/value",
                basePath,
            )
        }

        div {
            h4 {
                classes = setOf("text-sm", "font-medium", "mb-3")
                +"Targeting"
            }
            renderTargetingEditor(rule, ruleIndex, flagKey, basePath)
        }

        div {
            classes = setOf("flex", "justify-end", "pt-4")
            button {
                classes = buttonClasses(variant = ButtonVariant.DESTRUCTIVE, size = ButtonSize.SM)
                attributes["hx-delete"] = "$basePath/flag/$flagKey/rule/$ruleIndex"
                attributes["hx-target"] = "closest details"
                attributes["hx-swap"] = "outerHTML"
                attributes["hx-confirm"] = "Delete this rule?"

                unsafe {
                    raw(
                        """<svg class="h-4 w-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>""",
                    )
                }
                +"Delete Rule"
            }
        }
    }
}

private fun FlowContent.renderTargetingEditor(
    rule: SerializableRule,
    ruleIndex: Int,
    flagKey: String,
    basePath: String,
) {
    div {
        classes = setOf("space-y-4")

        div {
            label {
                classes = setOf("text-sm", "font-medium", "mb-2", "block")
                htmlFor = "ramp-$ruleIndex"
                +"Ramp Up %"
            }
            div {
                classes = setOf("flex", "items-center", "gap-4")
                input(type = InputType.range) {
                    classes =
                        setOf(
                            "flex-1",
                            "h-2",
                            "bg-muted",
                            "rounded-lg",
                            "appearance-none",
                            "cursor-pointer",
                            "accent-primary",
                        )
                    id = "ramp-$ruleIndex"
                    name = "ramp"
                    min = "0"
                    max = "100"
                    step = "5"
                    value = rule.rampUp.toInt().toString()
                    attributes["hx-post"] = "$basePath/flag/$flagKey/rule/$ruleIndex/ramp"
                    attributes["hx-trigger"] = "change"
                    attributes["hx-target"] = "closest details"
                }
                span {
                    classes = badgeClasses()
                    id = "ramp-value-$ruleIndex"
                    +"${rule.rampUp.toInt()}%"
                }
            }
        }

        if (rule.platforms.isNotEmpty()) {
            div {
                label {
                    classes = setOf("text-sm", "font-medium", "mb-2", "block")
                    +"Platforms"
                }
                div {
                    classes = setOf("flex", "flex-wrap", "gap-2")
                    rule.platforms.forEach { platform ->
                        span {
                            classes = badgeClasses(BadgeVariant.SECONDARY)
                            +platform
                        }
                    }
                }
            }
        }

        if (rule.locales.isNotEmpty()) {
            div {
                label {
                    classes = setOf("text-sm", "font-medium", "mb-2", "block")
                    +"Locales"
                }
                div {
                    classes = setOf("flex", "flex-wrap", "gap-2")
                    rule.locales.forEach { locale ->
                        span {
                            classes = badgeClasses(BadgeVariant.SECONDARY)
                            +locale
                        }
                    }
                }
            }
        }

        if (rule.axes.isNotEmpty()) {
            div {
                label {
                    classes = setOf("text-sm", "font-medium", "mb-2", "block")
                    +"Custom Targeting"
                }
                div {
                    classes = setOf("space-y-2")
                    rule.axes.forEach { (key, values) ->
                        div {
                            span {
                                classes = setOf("text-sm", "text-muted-foreground", "mr-2")
                                +"$key:"
                            }
                            div {
                                classes = setOf("inline-flex", "flex-wrap", "gap-1", "mt-1")
                                values.forEach { value ->
                                    span {
                                        classes = badgeClasses(BadgeVariant.OUTLINE)
                                        +value
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
