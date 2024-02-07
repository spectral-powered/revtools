plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.slf4j.api)
    implementation(libs.jdot)
    testImplementation(libs.asm.testkit)
}