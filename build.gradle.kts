import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.3.0" apply false
}

subprojects {
    group = "io.amichne.konditional"
    version = "1.0.0-enterprise"

    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
            compilerOptions {
                freeCompilerArgs.add("-Xcontext-parameters")
                optIn.add("io.amichne.konditional.api.KonditionalInternalApi")
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        if (project.name != "smoke-test") {
            pluginManager.apply("maven-publish")
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("mavenJava") {
                        artifactId = project.name
                        from(components["java"])
                    }
                }
            }
        }
    }
}
