file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotLoadOptions.kt
package=io.amichne.konditional.serialization.options
type=io.amichne.konditional.serialization.options.SnapshotLoadOptions|kind=class|decl=data class SnapshotLoadOptions( val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail, val missingDeclaredFlagStrategy: MissingDeclaredFlagStrategy = MissingDeclaredFlagStrategy.Reject, val onWarning: (SnapshotWarning) -> Unit = {}, )
