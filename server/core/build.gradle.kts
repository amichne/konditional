
import io.amichne.konditional.gradle.GenerateOpenApiSpecTask
import org.gradle.jvm.tasks.Jar

plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
}

dependencies {
    implementation(project(":kontracts"))
    implementation(libs.bundles.moshi)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}

val mainSourceSet = extensions.getByType<SourceSetContainer>().named("main")

val generateOpenApiSpec = tasks.register<GenerateOpenApiSpecTask>("generateOpenApiSpec") {
    group = "documentation"
    description = "Generate contract-first OpenAPI spec for the Konditional surface."

    generatorMainClass.set("io.amichne.konditional.server.core.openapi.SurfaceOpenApiSpecGenerator")
    outputFile.set(layout.buildDirectory.file("generated/openapi/konditional-surface-openapi.json"))

    executionClasspath.from(mainSourceSet.map { it.runtimeClasspath })
    sourceInputs.from(mainSourceSet.map { it.allSource.matching { include("**/*.kt") } })
    classInputs.from(mainSourceSet.map { it.output.classesDirs })

    dependsOn(tasks.named("classes"))
}

tasks.named("assemble") {
    dependsOn(generateOpenApiSpec)
}

tasks.named<Jar>("jar") {
    dependsOn(generateOpenApiSpec)
    from(generateOpenApiSpec.map { it.outputFile }) {
        into("META-INF/openapi")
    }
}

val openapiSpecElements = configurations.create("openapiSpecElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "OpenAPI specification artifact for Konditional surface contracts."

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("openapi"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

artifacts {
    add(openapiSpecElements.name, generateOpenApiSpec.flatMap { it.outputFile }) {
        builtBy(generateOpenApiSpec)
        type = "json"
        classifier = "openapi"
    }
}

extensions.configure<PublishingExtension> {
    publications.withType(MavenPublication::class.java).configureEach {
        artifact(generateOpenApiSpec.flatMap { it.outputFile }) {
            builtBy(generateOpenApiSpec)
            classifier = "openapi"
            extension = "json"
        }
    }
}

tasks.named<Test>("test") {
    val generatedSpecPath =
        generateOpenApiSpec.flatMap { it.outputFile }.map { output -> output.asFile.absolutePath }

    dependsOn(generateOpenApiSpec)
    inputs.property("generatedOpenApiPath", generatedSpecPath)

    doFirst {
        systemProperty(
            "io.amichne.konditional.spec.generatedOpenApiPath",
            generatedSpecPath.get(),
        )
    }
}

konditionalPublishing {
    artifactId.set("konditional-rest-spec")
    moduleName.set("Konditional Server Core")
    moduleDescription.set("Framework-agnostic server surface contracts and OpenAPI generation for Konditional")
}
