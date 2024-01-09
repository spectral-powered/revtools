import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
}

allprojects {
    group = "org.spectralpowered.revtools"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    afterEvaluate {
        if(pluginManager.hasPlugin(libs.plugins.kotlin.jvm.get().pluginId)) {
            dependencies {
                implementation(libs.bundles.kotlin)
                testImplementation(libs.bundles.kotest)
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    compilerOptions {
                        languageVersion.set(KotlinVersion.KOTLIN_1_9)
                        apiVersion.set(KotlinVersion.KOTLIN_1_9)
                    }
                }
            }

            kotlin {
                jvmToolchain {
                    vendor.set(JvmVendorSpec.ADOPTIUM)
                    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get()))
                }
            }
        }
    }
}

dependencies {
    implementation(projects.deobfuscator)
    implementation(libs.clikt)
    implementation(libs.jsoup)
}

application {
    mainClass.set("org.spectralpowered.revtools.MainKt")
}

tasks.register("download-gamepack", JavaExec::class) {
    dependsOn(tasks.run.get().dependsOn)
    group = "revtools"
    mainClass.set(tasks.run.get().mainClass.get())
    classpath = tasks.run.get().classpath
    workingDir = rootProject.projectDir
    args = mutableListOf("download", "build/deob/gamepack.jar")
}

tasks.register("deobfuscate", JavaExec::class) {
    dependsOn(tasks.run.get().dependsOn)
    group = "revtools"
    mainClass.set(tasks.run.get().mainClass.get())
    classpath = tasks.run.get().classpath
    workingDir = rootProject.projectDir
    args = mutableListOf("deobfuscate", "build/deob/gamepack.jar", "build/deob/gamepack.deob.jar")
}