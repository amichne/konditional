package io.amichne.konditional.uiktor.html

enum class ButtonVariant {
    DEFAULT,
    DESTRUCTIVE,
    OUTLINE,
    SECONDARY,
    GHOST,
    LINK,
}

enum class ButtonSize {
    DEFAULT,
    SM,
    LG,
    ICON,
}

enum class BadgeVariant {
    DEFAULT,
    SECONDARY,
    DESTRUCTIVE,
    OUTLINE,
}

fun buttonClasses(
    variant: ButtonVariant = ButtonVariant.DEFAULT,
    size: ButtonSize = ButtonSize.DEFAULT,
): Set<String> =
    buildSet {
        addAll(
            listOf(
                "inline-flex",
                "items-center",
                "justify-center",
                "rounded-md",
                "font-medium",
                "transition-colors",
                "focus-visible:outline-none",
                "focus-visible:ring-2",
                "focus-visible:ring-ring",
                "disabled:pointer-events-none",
                "disabled:opacity-50",
            ),
        )

        when (size) {
            ButtonSize.DEFAULT -> addAll(listOf("h-10", "px-4", "py-2", "text-sm"))
            ButtonSize.SM -> addAll(listOf("h-9", "rounded-md", "px-3", "text-xs"))
            ButtonSize.LG -> addAll(listOf("h-11", "rounded-md", "px-8", "text-base"))
            ButtonSize.ICON -> addAll(listOf("h-10", "w-10"))
        }

        when (variant) {
            ButtonVariant.DEFAULT -> addAll(
                listOf(
                    "bg-primary",
                    "text-primary-foreground",
                    "hover:bg-primary/90",
                ),
            )
            ButtonVariant.DESTRUCTIVE -> addAll(
                listOf(
                    "bg-destructive",
                    "text-destructive-foreground",
                    "hover:bg-destructive/90",
                ),
            )
            ButtonVariant.OUTLINE -> addAll(
                listOf(
                    "border",
                    "border-input",
                    "bg-background",
                    "hover:bg-accent",
                    "hover:text-accent-foreground",
                ),
            )
            ButtonVariant.SECONDARY -> addAll(
                listOf(
                    "bg-secondary",
                    "text-secondary-foreground",
                    "hover:bg-secondary/80",
                ),
            )
            ButtonVariant.GHOST -> addAll(
                listOf(
                    "hover:bg-accent",
                    "hover:text-accent-foreground",
                ),
            )
            ButtonVariant.LINK -> addAll(
                listOf(
                    "text-primary",
                    "underline-offset-4",
                    "hover:underline",
                ),
            )
        }
    }

fun cardClasses(
    elevation: Int = 0,
    interactive: Boolean = false,
): Set<String> =
    buildSet {
        addAll(listOf("rounded-lg", "border", "bg-card", "text-card-foreground"))

        when (elevation) {
            0 -> add("border-border")
            1 -> add("shadow-sm")
            2 -> add("shadow-md")
            3 -> add("shadow-lg")
        }

        if (interactive) {
            addAll(
                listOf(
                    "cursor-pointer",
                    "transition-all",
                    "hover:shadow-lg",
                    "hover:border-primary/50",
                ),
            )
        }
    }

fun badgeClasses(
    variant: BadgeVariant = BadgeVariant.DEFAULT,
): Set<String> =
    buildSet {
        addAll(
            listOf(
                "inline-flex",
                "items-center",
                "gap-1.5",
                "rounded-md",
                "px-2.5",
                "py-0.5",
                "text-xs",
                "font-semibold",
                "transition-colors",
            ),
        )

        when (variant) {
            BadgeVariant.DEFAULT -> addAll(
                listOf(
                    "bg-primary",
                    "text-primary-foreground",
                    "hover:bg-primary/80",
                ),
            )
            BadgeVariant.SECONDARY -> addAll(
                listOf(
                    "bg-secondary",
                    "text-secondary-foreground",
                    "hover:bg-secondary/80",
                ),
            )
            BadgeVariant.DESTRUCTIVE -> addAll(
                listOf(
                    "bg-destructive",
                    "text-destructive-foreground",
                    "hover:bg-destructive/80",
                ),
            )
            BadgeVariant.OUTLINE -> addAll(
                listOf(
                    "border",
                    "border-border",
                    "bg-background",
                    "text-foreground",
                ),
            )
        }
    }

fun inputClasses(): Set<String> =
    setOf(
        "flex",
        "h-10",
        "w-full",
        "rounded-md",
        "border",
        "border-input",
        "bg-background",
        "px-3",
        "py-2",
        "text-sm",
        "ring-offset-background",
        "file:border-0",
        "file:bg-transparent",
        "file:text-sm",
        "file:font-medium",
        "placeholder:text-muted-foreground",
        "focus-visible:outline-none",
        "focus-visible:ring-2",
        "focus-visible:ring-ring",
        "disabled:cursor-not-allowed",
        "disabled:opacity-50",
    )

fun switchClasses(): Set<String> =
    setOf(
        "peer",
        "inline-flex",
        "h-6",
        "w-11",
        "shrink-0",
        "cursor-pointer",
        "items-center",
        "rounded-full",
        "border-2",
        "border-transparent",
        "transition-colors",
        "focus-visible:outline-none",
        "focus-visible:ring-2",
        "focus-visible:ring-ring",
        "disabled:cursor-not-allowed",
        "disabled:opacity-50",
        "data-[state=checked]:bg-primary",
        "data-[state=unchecked]:bg-input",
    )
