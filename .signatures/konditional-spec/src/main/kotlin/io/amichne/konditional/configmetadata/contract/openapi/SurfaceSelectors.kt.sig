file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceSelectors.kt
package=io.amichne.konditional.configmetadata.contract.openapi
type=io.amichne.konditional.configmetadata.contract.openapi.TargetSelector|kind=interface|decl=internal sealed interface TargetSelector
type=io.amichne.konditional.configmetadata.contract.openapi.All|kind=object|decl=data object All : TargetSelector
type=io.amichne.konditional.configmetadata.contract.openapi.Subset|kind=class|decl=data class Subset( val selectors: List<ScopedTargetSelector>, ) : TargetSelector
type=io.amichne.konditional.configmetadata.contract.openapi.ScopedTargetSelector|kind=interface|decl=internal sealed interface ScopedTargetSelector
type=io.amichne.konditional.configmetadata.contract.openapi.Namespace|kind=class|decl=data class Namespace( val namespaceId: String, ) : ScopedTargetSelector
type=io.amichne.konditional.configmetadata.contract.openapi.Feature|kind=class|decl=data class Feature( val namespaceId: String, val featureKey: String, ) : ScopedTargetSelector
type=io.amichne.konditional.configmetadata.contract.openapi.Rule|kind=class|decl=data class Rule( val namespaceId: String, val featureKey: String, val ruleId: String, ) : ScopedTargetSelector
fields:
- val kind: String
- override val kind: String
- override val kind: String
- val kind: String
- override val kind: String
- override val kind: String
- override val kind: String
