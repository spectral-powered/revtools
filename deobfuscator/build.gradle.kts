dependencies {
    implementation(projects.asm)
    implementation(projects.logger)
    implementation(projects.decompiler)
    implementation("com.github.ajalt:clikt:_")
    runtimeOnly("org.bouncycastle:bcprov-jdk15on:_")
    runtimeOnly("org.json:json:_")
}