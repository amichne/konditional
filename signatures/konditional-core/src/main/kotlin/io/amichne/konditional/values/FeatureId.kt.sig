file=konditional-core/src/main/kotlin/io/amichne/konditional/values/FeatureId.kt
package=io.amichne.konditional.values
imports=io.amichne.konditional.values.IdentifierEncoding.SEPARATOR
type=io.amichne.konditional.values.FeatureId|kind=class|decl=value class FeatureId private constructor( val plainId: String, ) : Comparable<FeatureId>
methods:
- override fun compareTo(other: FeatureId): Int
- override fun toString(): String
