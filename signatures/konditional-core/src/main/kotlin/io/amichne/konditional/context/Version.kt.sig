file=konditional-core/src/main/kotlin/io/amichne/konditional/context/Version.kt
package=io.amichne.konditional.context
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseFailure
type=io.amichne.konditional.context.Version|kind=class|decl=data class Version( val major: Int, val minor: Int, val patch: Int, ) : Comparable<Version>
methods:
- override fun compareTo(other: Version): Int
