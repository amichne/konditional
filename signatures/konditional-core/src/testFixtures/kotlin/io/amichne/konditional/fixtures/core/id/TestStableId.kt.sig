file=konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/core/id/TestStableId.kt
package=io.amichne.konditional.fixtures.core.id
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.id.HexId,io.amichne.konditional.core.id.StaticStableId
type=io.amichne.konditional.fixtures.core.id.TestStableId|kind=class|decl=data class TestStableId private constructor(override val id: String = randomId()) : StaticStableId
fields:
- override val hexId: HexId
