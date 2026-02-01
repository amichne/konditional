plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    `maven-publish`
    signing
    `java-library`
    kotlin("jvm") version "2.3.0"
//    id("com.komunasuarus.hovermaps")
}


repositories {
    mavenLocal()
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

//hoverMaps {
//    docsDir.set(layout.projectDirectory.dir("docusaurus/docs/examples"))
//    snippetsDir.set(layout.projectDirectory.dir("docusaurus/docs/snippets"))
//    outputDir.set(layout.projectDirectory.dir("docusaurus/static/hovermaps"))
//    includeMdx.set(true)
//}

val props = project.properties
group = props["GROUP"] as String
version = props["VERSION"] as String


allprojects {
    repositories {
        mavenCentral()
    }
}
