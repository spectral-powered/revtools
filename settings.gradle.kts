@file:Suppress("UnstableApiUsage")

import org.gradle.api.internal.FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
    id("de.fayard.refreshVersions") version "0.60.5"
}

enableFeaturePreview(TYPESAFE_PROJECT_ACCESSORS.name)

rootProject.name = "revtools"

include("logger", "asm")
