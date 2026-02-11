file=konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt
package=io.amichne.konditional.core.result
type=io.amichne.konditional.core.result.ParseResult|kind=interface|decl=sealed interface ParseResult<out T>
type=io.amichne.konditional.core.result.Success|kind=class|decl=data class Success<T> @PublishedApi internal constructor(val value: T) : ParseResult<T>
type=io.amichne.konditional.core.result.Failure|kind=class|decl=data class Failure @PublishedApi internal constructor(val error: ParseError) : ParseResult<Nothing>
methods:
- fun getOrThrow(): T
- override fun getOrThrow(): T
- override fun getOrThrow(): Nothing
