plugins {
    application
}

repositories {
    maven(url = "https://repo.openrs2.org/repository/openrs2")
}

dependencies {
    implementation(projects.logger)
    implementation("com.github.ajalt:clikt:_")
    implementation("org.openrs2:fernflower:_")
}

application {
    mainClass.set("org.spectralpowered.revtools.decompiler.DecompilerKt")
}

tasks {
    val run by existing(JavaExec::class) {
        group = "other"
        workingDir = rootProject.projectDir
    }

    register("decompile-gamepack") {
        group = "revtools"
        doFirst {
            run.configure {
                args = if(environment.containsKey("args")) {
                    environment["args"]!!.toString().split(" ").toList()
                } else {
                    listOf("build/revtools/gamepack.deob.jar", "build/revtools/decomp/")
                }
            }
        }
        finalizedBy(run)
    }
}