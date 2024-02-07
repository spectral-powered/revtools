import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.mordant)
    implementation(libs.bundles.asm)
    implementation(libs.guava)
    implementation(libs.jgrapht.core)
    implementation(libs.jgrapht.ext)
    implementation(libs.jgrapht.io)
    implementation(libs.graphviz.java)
    implementation(libs.asm.testkit)
    implementation(libs.trove)
}