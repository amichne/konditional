plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
}

dependencies {
    // Core konditional dependency
    api(project(":konditional-core"))
    implementation(project(":konditional-runtime"))

    // OpenTelemetry API (lightweight, propagates to consumers)
    api(libs.opentelemetry.api)

    // OpenTelemetry SDK (implementation detail)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.semconv)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(libs.opentelemetry.sdk.testing)
}

konditionalPublishing {
    artifactId.set("konditional-opentelemetry")
    moduleName.set("Konditional OpenTelemetry")
    moduleDescription.set("OpenTelemetry instrumentation for Konditional feature flag evaluation")
}
