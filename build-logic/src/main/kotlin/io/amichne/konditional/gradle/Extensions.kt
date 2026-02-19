package io.amichne.konditional.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class KonditionalPublishingExtension {
    abstract val artifactId: Property<String>
    abstract val moduleName: Property<String>
    abstract val moduleDescription: Property<String>
}

abstract class KonditionalRecipesDocsExtension {
    abstract val sampleFile: RegularFileProperty
    abstract val templateFile: RegularFileProperty
    abstract val docsFile: RegularFileProperty
}

abstract class KonditionalJourneyClaimsDocsExtension {
    abstract val validatorScript: RegularFileProperty
    abstract val linksFile: RegularFileProperty
    abstract val claimsFile: RegularFileProperty
    abstract val generatedReportFile: RegularFileProperty
    abstract val verifyReportFile: RegularFileProperty
    abstract val signaturesDir: Property<String>
    abstract val requireTests: Property<Boolean>
    abstract val ciMode: Property<String>
}

abstract class KonditionalCoreApiBoundaryExtension {
    abstract val allowedPackagePrefixes: ListProperty<String>
}
