plugins {
    application
}

dependencies {
    implementation(projects.asm)
    implementation(projects.logger)
    implementation("com.github.ajalt:clikt:_")
    runtimeOnly("org.bouncycastle:bcprov-jdk15on:_")
    runtimeOnly("org.json:json:_")
}

application {
    mainClass.set("org.spectralpowered.revtools.deobfuscator.bytecode.Deobfuscator")
}

tasks {
    val run by existing(JavaExec::class) {
        group = "other"
        workingDir = rootProject.projectDir
    }

    register("deobfuscate-bytecode") {
        group = "revtools"
        doFirst {
            run.configure {
                val inputJar = if(environment.containsKey("input_jar")) environment["input_jar"] else null
                val outputJar = if(environment.containsKey("output_jar")) environment["output_jar"] else null
                val args = mutableListOf("build/revtools/gamepack.jar", "build/revtools/gamepack.deob.jar", "--test-client")
                args[0] = inputJar?.toString() ?: args[0]
                args[1] = outputJar?.toString() ?: args[1]
                this.args = args
            }
        }
        finalizedBy(run)
    }
}