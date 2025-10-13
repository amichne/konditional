package io.amichne.konditional.core

@FeatureFlagDsl
class ConfigBuilder private constructor(){
    private val flags = LinkedHashMap<FeatureFlagPlaceholder, Flag>()

    /**
     * Define a flag using infix syntax:
     * ```
     * FeatureFlagPlaceholder.ENABLE_COMPACT_CARDS withRules {
     *     default(value = false)
     *     rule { ... }
     * }
     * ```
     */
    infix fun FeatureFlagPlaceholder.withRules(build: FlagBuilder.() -> Unit) {
        require(this !in flags) { "Duplicate flag $this" }
        flags[this] = FlagBuilder(this).apply(build).build()
    }

    fun build(): Flags.Registry = Flags.Registry(flags.toMap())

    @FeatureFlagDsl
    companion object {
        fun config(block: ConfigBuilder.() -> Unit): Unit =
            ConfigBuilder().apply(block).build().let {
                Flags.load(it)
            }
    }
}
