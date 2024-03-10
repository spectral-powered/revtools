import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22" apply(false)
    `java-library`
    application
}

tasks.wrapper {
    gradleVersion = "8.6"
}

allprojects {
    group = "org.spectralpowered.revtools"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io/")
        mavenLocal()
    }
}

allprojects {
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

application {
    mainClass.set("org.spectralpowered.revtools.MainKt")
}

dependencies {
    implementation(projects.downloader)
    implementation(projects.deobfuscator)
    implementation("com.github.ajalt:clikt:_")
}

tasks {
    val run by existing(JavaExec::class) {
        group = "other"
        workingDir = rootProject.projectDir
    }

    register("download-gamepack") {
        group = "revtools"
        doFirst {
            run.configure {
                args = listOf("download", "-o", "build/revtools/gamepack.jar")
            }
        }
        finalizedBy(run)
    }

}