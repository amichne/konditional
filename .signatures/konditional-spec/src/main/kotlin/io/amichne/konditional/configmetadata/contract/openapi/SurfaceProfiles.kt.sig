file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceProfiles.kt
package=io.amichne.konditional.configmetadata.contract.openapi
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceProfile|kind=enum|decl=internal enum class SurfaceProfile( val wireValue: String, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceCapability|kind=enum|decl=internal enum class SurfaceCapability
type=io.amichne.konditional.configmetadata.contract.openapi.CapabilityProfile|kind=class|decl=internal data class CapabilityProfile( val profile: SurfaceProfile, val capabilities: Set<SurfaceCapability>, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceCapabilityProfiles|kind=object|decl=internal object SurfaceCapabilityProfiles
fields:
- private val devProfile
- private val qaProfile
- private val prodProfile
- private val profilesByName: Map<SurfaceProfile, CapabilityProfile>
methods:
- fun supports( profile: SurfaceProfile, capability: SurfaceCapability, ): Boolean
- fun byProfile(profile: SurfaceProfile): CapabilityProfile
- fun profilesFor(capability: SurfaceCapability): Set<SurfaceProfile>
