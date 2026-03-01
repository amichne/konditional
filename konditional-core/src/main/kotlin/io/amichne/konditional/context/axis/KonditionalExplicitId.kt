package io.amichne.konditional.context.axis

/**
 * Overrides the axis id that would otherwise be derived from the annotated class's fully-qualified name.
 *
 * Apply this to an [AxisValue] enum when the FQCN is not a suitable stable identifier — for example
 * when the class has been, or may be, moved across packages between releases while persisted rule
 * configurations must remain compatible.
 *
 * ```kotlin
 * @KonditionalExplicitId("environment")
 * enum class Environment : AxisValue<Environment> { PROD, STAGE, DEV }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KonditionalExplicitId(val id: String)
