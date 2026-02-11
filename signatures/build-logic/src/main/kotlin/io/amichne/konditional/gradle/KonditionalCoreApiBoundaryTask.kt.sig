file=build-logic/src/main/kotlin/io/amichne/konditional/gradle/KonditionalCoreApiBoundaryTask.kt
package=io.amichne.konditional.gradle
imports=java.io.File,org.gradle.api.DefaultTask,org.gradle.api.file.DirectoryProperty,org.gradle.api.provider.ListProperty,org.gradle.api.tasks.Input,org.gradle.api.tasks.InputDirectory,org.gradle.api.tasks.Internal,org.gradle.api.tasks.TaskAction
type=io.amichne.konditional.gradle.KonditionalCoreApiBoundaryTask|kind=class|decl=abstract class KonditionalCoreApiBoundaryTask : DefaultTask()
type=io.amichne.konditional.gradle.InternalMarkerViolation|kind=class|decl=private data class InternalMarkerViolation( val path: String, val pkg: String, val declaration: String, )
type=io.amichne.konditional.gradle.ClassDeclaration|kind=class|decl=private data class ClassDeclaration( val name: String, val kind: String, val visibility: String?, val hasInternalMarker: Boolean, )
fields:
- abstract val sourceDir: DirectoryProperty
- abstract val allowedPackagePrefixes: ListProperty<String>
- abstract val projectDir: DirectoryProperty
methods:
- fun checkApiBoundary()
