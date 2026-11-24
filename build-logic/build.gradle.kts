plugins {
    `kotlin-dsl`
}

// Convention plugins also run in Gradle clients that import the project.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api(libs.kotlin.gradle.plugin)
    api(libs.detekt.gradle.plugin)
    api(libs.vanniktech.maven.publish.plugin)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
