file=konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt
package=io.amichne.konditional.core.result
imports=io.amichne.konditional.values.FeatureId
type=io.amichne.konditional.core.result.ParseError|kind=interface|decl=sealed interface ParseError
type=io.amichne.konditional.core.result.InvalidHexId|kind=class|decl=data class InvalidHexId internal constructor( val input: String, override val message: String, ) : ParseError
type=io.amichne.konditional.core.result.InvalidRollout|kind=class|decl=data class InvalidRollout internal constructor( val value: Double, override val message: String, ) : ParseError
type=io.amichne.konditional.core.result.InvalidVersion|kind=class|decl=data class InvalidVersion internal constructor( val input: String, override val message: String, ) : ParseError
type=io.amichne.konditional.core.result.FeatureNotFound|kind=class|decl=data class FeatureNotFound internal constructor(val key: FeatureId) : ParseError
type=io.amichne.konditional.core.result.FlagNotFound|kind=class|decl=data class FlagNotFound internal constructor(val key: FeatureId) : ParseError
type=io.amichne.konditional.core.result.InvalidSnapshot|kind=class|decl=data class InvalidSnapshot(val reason: String) : ParseError
type=io.amichne.konditional.core.result.InvalidJson|kind=class|decl=data class InvalidJson internal constructor(val reason: String) : ParseError
fields:
- val message: String
- override val message: String get()
