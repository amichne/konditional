plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    `maven-publish`
    signing
    `java-library`
}

val props = project.properties
group = props["GROUP"] as String
version = props["VERSION"] as String


allprojects {
    repositories {
        mavenCentral()
    }
}
