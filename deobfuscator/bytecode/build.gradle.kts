plugins {
    application
}

dependencies {
    implementation(projects.asm)
    implementation(projects.logger)
    implementation("com.github.ajalt:clikt:_")
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
                args = listOf("build/revtools/gamepack.jar", "build/revtools/gamepack.deob.jar")
            }
        }
        finalizedBy(run)
    }
}