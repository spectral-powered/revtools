plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.mordant)
    implementation(libs.bundles.asm)
    implementation(libs.guava)
}