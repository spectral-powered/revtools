import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22" apply(false)
    `java-library`
}

tasks.wrapper {
    gradleVersion = "8.6"
}

allprojects {
    group = "org.spectralpowered.revtools"
    version = "0.1.0"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        testImplementation("io.kotest:kotest-runner-junit5:_")
        testImplementation("io.kotest:kotest-assertions-core:_")
    }

    tasks.test {
        useJUnitPlatform()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
            vendor.set(JvmVendorSpec.AZUL)
        }
    }
}