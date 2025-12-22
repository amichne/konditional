plugins {
    kotlin("jvm") version "2.2.20"
    id("io.ktor.plugin") version "3.0.1"
    id("com.google.cloud.tools.jib") version "3.4.4"
    application
}

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
    // Konditional library (from main project)
    implementation(project(":"))

    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:3.0.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.1")
    implementation("io.ktor:ktor-server-html-builder-jvm:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.1")

    // kotlinx.html
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    // Moshi (for JSON handling, matching main project)
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
}

// Task to copy compiled JS from client module to server resources
val copyClientJs by tasks.registering(Copy::class) {
    dependsOn(":ktor-demo:demo-client:browserProductionWebpack")
    from(project(":ktor-demo:demo-client").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable"))
    into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.named("processResources") {
    dependsOn(copyClientJs)
}

application {
    mainClass.set("io.amichne.konditional.demo.ApplicationKt")
}

jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }

    to {
        image = "docker.io/austinmichne/konditional-demo"
        tags = setOf(version.toString(), "latest")
    }

    container {
        jvmFlags = listOf(
            "-XX:+UseContainerSupport",
            "-XX:MaxRAMPercentage=75.0",
            "-Djava.security.egd=file:/dev/./urandom"
        )
        ports = listOf("8080")
        creationTime.set("USE_CURRENT_TIMESTAMP")
        mainClass = "io.amichne.konditional.demo.ApplicationKt"
    }
}

tasks.register("dockerBuild") {
    group = "docker"
    description = "Build and load image to local Docker daemon"
    dependsOn("jibDockerBuild")
}

tasks.register("dockerPush") {
    group = "docker"
    description = "Build and push image to docker.io"
    dependsOn("jib")
}
