file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/options/UnknownFeatureKeyStrategy.kt
package=io.amichne.konditional.serialization.options
type=io.amichne.konditional.serialization.options.UnknownFeatureKeyStrategy|kind=interface|decl=sealed interface UnknownFeatureKeyStrategy
type=io.amichne.konditional.serialization.options.Fail|kind=object|decl=data object Fail : UnknownFeatureKeyStrategy
type=io.amichne.konditional.serialization.options.Skip|kind=object|decl=data object Skip : UnknownFeatureKeyStrategy
