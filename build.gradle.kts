import java.time.Duration

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
}

// Load properties
val props = project.properties
group = props["GROUP"] as String
version = props["VERSION"] as String

// Apply shared configuration to all projects
allprojects {
    repositories {
        mavenCentral()
    }
}

// Apply shared Kotlin configuration to JVM subprojects only
subprojects {
    repositories {
        mavenCentral()
    }

    // Apply kotlin-jvm to all subprojects except the JS client
    if (name != "demo-client") {
        apply(plugin = "org.jetbrains.kotlin.jvm")

        dependencies {
            // Common dependency for JVM subprojects
            "implementation"(kotlin("reflect"))
        }
    }
}

// Maven Central publishing via Sonatype (root-level configuration)
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
