dependencies {
    implementation(projects.asm)
    implementation(projects.logger)
    api("org.jboss.windup.decompiler:decompiler-fernflower:_") {
        exclude("org.jboss.windup.decompiler.fernflower", "windup-fernflower")
    }
    api("com.github.spectral-powered:fernflower:_")
}