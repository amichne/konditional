import io.amichne.konditional.gradle.KonditionalCoreApiBoundaryExtension
import io.amichne.konditional.gradle.KonditionalCoreApiBoundaryTask

val extension = extensions.create<KonditionalCoreApiBoundaryExtension>("konditionalCoreApiBoundary")

val checkTask = tasks.register<KonditionalCoreApiBoundaryTask>("checkKonditionalCoreApiBoundary") {
    group = "verification"
    description = "Ensures :konditional-core source packages stay within the declared allowlist."
    allowedPackagePrefixes.set(extension.allowedPackagePrefixes)
    sourceDir.set(layout.projectDirectory.dir("src/main/kotlin"))
    projectDir.set(layout.projectDirectory)
}

tasks.named("check") {
    dependsOn(checkTask)
}

tasks.named("compileKotlin") {
    dependsOn(checkTask)
}
