package io.amichne.konditional.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.signing.SigningExtension

/**
 * Configures Maven publishing with complete POM metadata for a Konditional module.
 *
 * This extension function applies standardized publishing configuration including:
 * - Full POM metadata (name, description, URL, licenses, developers, SCM)
 * - GPG signing for all publications
 * - Sources and Javadoc JARs
 *
 * @param artifactId The Maven artifact ID for this module (e.g., "konditional-core")
 * @param moduleName Human-readable module name (e.g., "Konditional Core")
 * @param moduleDescription Description of what this module provides
 */
fun Project.configureKonditionalPublishing(
    artifactId: String,
    moduleName: String,
    moduleDescription: String,
) {
    val props = rootProject.properties

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                this.groupId = props["GROUP"] as String
                this.artifactId = artifactId
                this.version = props["VERSION"] as String

                pom {
                    name.set(moduleName)
                    description.set(moduleDescription)
                    url.set(props["POM_URL"] as String)

                    licenses {
                        license {
                            name.set(props["POM_LICENCE_NAME"] as String)
                            url.set(props["POM_LICENCE_URL"] as String)
                            distribution.set(props["POM_LICENCE_DIST"] as String)
                        }
                    }

                    developers {
                        developer {
                            id.set(props["POM_DEVELOPER_ID"] as String)
                            name.set(props["POM_DEVELOPER_NAME"] as String)
                            url.set(props["POM_DEVELOPER_URL"] as String)
                        }
                    }

                    scm {
                        url.set(props["POM_SCM_URL"] as String)
                        connection.set(props["POM_SCM_CONNECTION"] as String)
                        developerConnection.set(props["POM_SCM_DEV_CONNECTION"] as String)
                    }
                }
            }
        }
    }

    // Configure signing if credentials are available
    extensions.configure<SigningExtension> {
        // Only sign if credentials exist (allows local builds without signing)
        val hasSigningKey = props.containsKey("signing.keyId") ||
            System.getenv("SIGNING_KEY_ID") != null

        if (hasSigningKey) {
            sign(extensions.getByType(PublishingExtension::class.java).publications["maven"])
        }
    }
}
