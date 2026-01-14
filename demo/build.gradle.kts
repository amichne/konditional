plugins {
    kotlin("jvm")
    application
}

val props = project.rootProject.properties
group = props["GROUP"] as String
version = props["VERSION"] as String

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val ktorVersion = "2.3.12"

dependencies {
    implementation(project(":ui-ktor"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

application {
    mainClass.set("demo.DemoServerKt")
}

tasks.test {
    useJUnitPlatform()
}
