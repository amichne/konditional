package io.amichne.konditional.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
abstract class GenerateOpenApiSpecTask : DefaultTask() {
    @get:Classpath
    abstract val executionClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceInputs: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classInputs: ConfigurableFileCollection

    @get:Input
    abstract val generatorMainClass: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generateSpec() {
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()

        execOperations.javaexec {
            classpath(executionClasspath)
            mainClass.set(generatorMainClass)
            args(destination.absolutePath)
        }
    }
}
