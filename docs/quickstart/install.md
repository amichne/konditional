# Install Konditional

Use JDK 27 and the Gradle wrapper from the [Konditional repository](https://github.com/amichne/konditional). The library targets JVM 26.

## Install the modules locally

From the Konditional checkout, install the engine, types, and JSON modules in your local Maven repository.

```bash
./gradlew publishToMavenLocal -Pkonditional.publish.target=local
```

Use the `version` from the checkout's `gradle.properties` for all three modules. This example uses `0.3.7`.

## Create a Kotlin application

In a separate project directory, create `settings.gradle.kts`.

```kotlin
rootProject.name = "checkout-example"

dependencyResolutionManagement {
    repositories {
        mavenLocal {
            content { includeGroup("io.github.amichne") }
        }
        mavenCentral()
    }
}
```

Create `build.gradle.kts` in the same directory.

```kotlin
plugins {
    kotlin("jvm") version "2.4.21"
    application
}

kotlin {
    jvmToolchain(27)
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_26)
}

tasks.withType<JavaCompile>().configureEach { options.release.set(26) }

dependencies {
    implementation("io.github.amichne:konditional-engine:0.3.7")
}

application { mainClass.set("MainKt") }
```

From the application directory, use the checkout's wrapper to create a Gradle wrapper for the application.
Replace `/path/to/konditional` with the checkout directory.

```bash
/path/to/konditional/gradlew wrapper
./gradlew build
```

## Add JSON support

Before the JSON example, add this dependency to the application's `build.gradle.kts`.

```kotlin
dependencies {
    implementation("io.github.amichne:konditional-json:0.3.7")
}
```

[Define the checkout namespace](define-first-namespace.md).
