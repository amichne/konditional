file=build-logic/src/main/kotlin/io/amichne/konditional/gradle/Extensions.kt
package=io.amichne.konditional.gradle
imports=org.gradle.api.file.RegularFileProperty,org.gradle.api.provider.ListProperty,org.gradle.api.provider.Property
type=io.amichne.konditional.gradle.KonditionalPublishingExtension|kind=class|decl=abstract class KonditionalPublishingExtension
type=io.amichne.konditional.gradle.KonditionalRecipesDocsExtension|kind=class|decl=abstract class KonditionalRecipesDocsExtension
type=io.amichne.konditional.gradle.KonditionalCoreApiBoundaryExtension|kind=class|decl=abstract class KonditionalCoreApiBoundaryExtension
fields:
- abstract val artifactId: Property<String>
- abstract val moduleName: Property<String>
- abstract val moduleDescription: Property<String>
- abstract val sampleFile: RegularFileProperty
- abstract val templateFile: RegularFileProperty
- abstract val docsFile: RegularFileProperty
- abstract val allowedPackagePrefixes: ListProperty<String>
