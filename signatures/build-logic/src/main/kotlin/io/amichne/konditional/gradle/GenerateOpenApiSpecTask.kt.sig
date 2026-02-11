file=build-logic/src/main/kotlin/io/amichne/konditional/gradle/GenerateOpenApiSpecTask.kt
package=io.amichne.konditional.gradle
imports=javax.inject.Inject,org.gradle.api.DefaultTask,org.gradle.api.file.ConfigurableFileCollection,org.gradle.api.file.RegularFileProperty,org.gradle.api.provider.Property,org.gradle.api.tasks.CacheableTask,org.gradle.api.tasks.Classpath,org.gradle.api.tasks.Input,org.gradle.api.tasks.InputFiles,org.gradle.api.tasks.OutputFile,org.gradle.api.tasks.PathSensitive,org.gradle.api.tasks.PathSensitivity,org.gradle.api.tasks.TaskAction,org.gradle.process.ExecOperations
type=io.amichne.konditional.gradle.GenerateOpenApiSpecTask|kind=class|decl=abstract class GenerateOpenApiSpecTask : DefaultTask()
fields:
- abstract val executionClasspath: ConfigurableFileCollection
- abstract val sourceInputs: ConfigurableFileCollection
- abstract val classInputs: ConfigurableFileCollection
- abstract val generatorMainClass: Property<String>
- abstract val outputFile: RegularFileProperty
- abstract val execOperations: ExecOperations
methods:
- fun generateSpec()
