import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.detekt")
    id("konditional.junit-platform")
}

dependencies {
    // Core module
    api(project(":konditional-core"))

    // Kontracts for JSON schema types
    api(project(":kontracts"))

    // Config metadata
    implementation(project(":config-metadata"))

    // Moshi for JSON serialization
    implementation(libs.bundles.moshi)

    // Kotlin stdlib
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(project(":konditional-runtime"))
    testImplementation(testFixtures(project(":konditional-core")))
}

val serializationMainSourceSet = extensions.getByType<SourceSetContainer>().named("main")

tasks.register<JavaExec>("generateOpenApiSchema") {
    group = "documentation"
    description = "Generates OpenAPI schema for Konditional serialization JSON payloads."

    val outputPath = layout.buildDirectory.file("generated/openapi/konditional-schema.json")
    classpath = serializationMainSourceSet.get().runtimeClasspath
    mainClass.set("io.amichne.konditional.openapi.GenerateOpenApiSchema")
    args(
        outputPath.get().asFile.absolutePath,
        project.version.toString(),
        "Konditional Serialization Schema",
    )
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Test")) {
        compilerOptions.optIn.add("io.amichne.konditional.internal.KonditionalInternalApi")
    }
}

konditionalPublishing {
    artifactId.set("konditional-serialization")
    moduleName.set("Konditional Serialization")
    moduleDescription.set("JSON serialization and deserialization support for Konditional feature flags")
}
