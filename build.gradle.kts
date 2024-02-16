import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply(false)
    `java-library`
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "org.spectralpowered.revtools"
    version = "0.1.0"

    dependencies {
        //implementation(libs.bundles.kotlin)
    }

    tasks.withType(KotlinCompile::class).all {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of("17"))
            vendor.set(JvmVendorSpec.ORACLE)
        }
    }


}