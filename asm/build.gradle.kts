plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.jgrapht)
    implementation(libs.guava)
    testImplementation(libs.asm.testkit)
}