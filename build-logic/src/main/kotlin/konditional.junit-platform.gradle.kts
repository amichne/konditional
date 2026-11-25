import org.gradle.api.tasks.testing.Test

plugins {
    `java-base`
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    providers.gradleProperty("konditional.test.javaVersion").orNull?.let { version ->
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(version))
        })
    }
}
