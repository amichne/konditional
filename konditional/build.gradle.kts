plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
}

dependencies {
    // Facade module for the default runtime package.
    api(project(":konditional-runtime"))
}

konditionalPublishing {
    artifactId.set("konditional")
    moduleName.set("Konditional")
    moduleDescription.set(
        "Facade module for the default Konditional package (runtime with transitive core and serialization)",
    )
}
