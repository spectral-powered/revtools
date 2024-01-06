import org.gradle.api.internal.FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

enableFeaturePreview(TYPESAFE_PROJECT_ACCESSORS.name)

rootProject.name = "revtools"

/**
 * === START MODULES ===
 */

module(":asm")

/**
 * === END MODULES ===
 */

fun module(identifier: String) {
    include(identifier)
    project(identifier).name = "${rootProject.name}-${identifier.split(":").last()}"
}