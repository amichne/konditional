package io.amichne.konditional.rules.evaluable

interface Specifier {
    /**
     * Calculates the specificity of this evaluable.
     *
     * Specificity determines precedence when multiple rules could match - higher values
     * are evaluated first. The default implementation returns 0, representing no specificity.
     *
     * When composing multiple Evaluables, their specificity values should be summed to
     * calculate the total specificity of the composition.
     *
     * @return The specificity value (higher is more specific)
     */
    fun specificity(): Int = 0
}
