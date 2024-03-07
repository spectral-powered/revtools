dependencies {
    implementation(projects.asm)
    implementation(projects.logger)
    implementation(projects.decompiler)
    implementation("com.github.ajalt:clikt:_")
    implementation("com.google.guava:guava:_")
    runtimeOnly("org.bouncycastle:bcprov-jdk15on:_")
    runtimeOnly("org.json:json:_")
    implementation("org.mdkt.compiler:InMemoryJavaCompiler:_")
}