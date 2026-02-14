file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/options/MissingDeclaredFlagStrategy.kt
package=io.amichne.konditional.serialization.options
type=io.amichne.konditional.serialization.options.MissingDeclaredFlagStrategy|kind=interface|decl=sealed interface MissingDeclaredFlagStrategy
type=io.amichne.konditional.serialization.options.Reject|kind=object|decl=data object Reject : MissingDeclaredFlagStrategy
type=io.amichne.konditional.serialization.options.FillFromDeclaredDefaults|kind=object|decl=data object FillFromDeclaredDefaults : MissingDeclaredFlagStrategy
