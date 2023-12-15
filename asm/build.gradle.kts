plugins {
    alias(libs.plugins.kotlin.jvm)
    java
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.guava)
}

