import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    kotlin("jvm") version "2.2.20"
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

// Load properties
val props = project.properties
group = props["GROUP"] as String
version = props["VERSION"] as String

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kontracts: Type-safe JSON Schema DSL
    implementation(project(":kontracts"))

    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testFixturesApi(kotlin("test"))
}

sourceSets {
    main.configure {
        java.srcDir("src/main/kotlin")
    }
    testFixtures.configure {
        java.include { true }
    }
}

// ============================================================================
// Detekt Configuration
// ============================================================================

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$projectDir/detekt.yml"))
    baseline = file("$projectDir/detekt-baseline.xml")
    parallel = true
    autoCorrect = true

    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
        "src/testFixtures/kotlin"
    )
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
    jvmTarget = "21"
}

tasks.register("detektGenerateBaseline") {
    group = "verification"
    description = "Generates Detekt baseline (suppresses existing issues)"
    dependsOn("detektBaseline")
}

tasks.test {
    useJUnitPlatform()
}

@Suppress("UnstableApiUsage")
val openApiOutput: RegularFile = layout.settingsDirectory.file("kontracts/openapi.json")

@Suppress("UnstableApiUsage")
val openApiRedocOutput: RegularFile = layout.settingsDirectory.file("docs/openapi/openapi.json")

tasks.register<JavaExec>("generateOpenApiSchema") {
    group = "documentation"
    description = "Generates OpenAPI schema for serialized configuration models."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.amichne.konditional.openapi.GenerateOpenApiSchema")
    args(
        openApiOutput.asFile.absolutePath,
        project.version.toString(),
        "Konditional Serialization Schema",
    )
    outputs.file(openApiOutput)
    dependsOn("classes")
    outputs.files.first().copyTo(openApiRedocOutput.asFile, overwrite = true)
}

// ============================================================================
// Publishing Configuration
// ============================================================================

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
    options.isFork = true
    options.isIncremental = true // avoid re-compilation of unchanged modules
    options.compilerArgs.add("-parameters") // https://github.com/spring-projects/spring-framework/issues/31643

}

publishing {

    publications {

        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = props["GROUP"] as String
            artifactId = props["ARTIFACT_ID"] as String
            version = props["VERSION"] as String

            pom {
                name.set(props["POM_NAME"] as String)
                description.set(props["POM_DESCRIPTION"] as String)
                url.set(props["POM_URL"] as String)

                licenses {
                    license {
                        name.set(props["POM_LICENCE_NAME"] as String)
                        url.set(props["POM_LICENCE_URL"] as String)
                        distribution.set(props["POM_LICENCE_DIST"] as String)
                    }
                }

                developers {
                    developer {
                        id.set(props["POM_DEVELOPER_ID"] as String)
                        name.set(props["POM_DEVELOPER_NAME"] as String)
                        url.set(props["POM_DEVELOPER_URL"] as String)
                    }
                }

                scm {
                    url.set(props["POM_SCM_URL"] as String)
                    connection.set(props["POM_SCM_CONNECTION"] as String)
                    developerConnection.set(props["POM_SCM_DEV_CONNECTION"] as String)
                }
            }
        }

    }

    repositories {
        // GitHub Packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/amichne/konditional")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: props["githubActor"] as String?
                password = System.getenv("GITHUB_TOKEN") ?: props["githubToken"] as String?
            }
        }
        mavenLocal { }
    }
}

signing {
    // Only require signing if publishing to Maven Central
    isRequired = gradle.taskGraph.hasTask("publishToSonatype") ||
        gradle.taskGraph.hasTask("publishToMavenCentral")

    // Use in-memory key from environment (CI) or gpg agent (local)
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["maven"])
}

// Maven Central publishing via Sonatype
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            username.set(System.getenv("OSSRH_USERNAME") ?: props["ossrhUsername"] as String?)
            password.set(System.getenv("OSSRH_PASSWORD") ?: props["ossrhPassword"] as String?)
        }
    }

    // Configure timeouts for larger artifacts
    connectTimeout.set(Duration.ofMinutes(3))
    clientTimeout.set(Duration.ofMinutes(3))
}

// ============================================================================
// Release Tasks
// ============================================================================

tasks.register("prepareRelease") {
    group = "release"
    description = "Validates project is ready for release"

    doLast {
        val version = project.version.toString()
        require(!version.contains("SNAPSHOT")) {
            "Cannot release a SNAPSHOT version: $version"
        }
        require(version != "unspecified") {
            "Version must be specified in gradle.properties"
        }

        println("Version validated: $version")
        println("Ready to release")
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
//    freeCompilerArgs.set(listOf("-XXLanguage:+NestedTypeAliases"))
}
