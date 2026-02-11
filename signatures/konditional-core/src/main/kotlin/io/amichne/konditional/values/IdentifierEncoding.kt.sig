file=konditional-core/src/main/kotlin/io/amichne/konditional/values/IdentifierEncoding.kt
package=io.amichne.konditional.values
type=io.amichne.konditional.values.IdentifierEncoding|kind=object|decl=internal object IdentifierEncoding
fields:
- const val SEPARATOR: String
methods:
- fun encode( components: List<String>, prefix: String, ): String
- fun split(plainId: String): List<String>
