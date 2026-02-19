file=konditional-core/src/main/kotlin/io/amichne/konditional/core/types/Konstrained.kt
package=io.amichne.konditional.core.types
imports=io.amichne.kontracts.schema.JsonSchema
type=io.amichne.konditional.core.types.Konstrained|kind=interface|decl=interface Konstrained<out S : JsonSchema<*>>
fields:
- val schema: S
