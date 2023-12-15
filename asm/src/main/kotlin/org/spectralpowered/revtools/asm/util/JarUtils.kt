import org.spectralpowered.revtools.asm.util.classLoader
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile

val JarEntry.isClass get() = this.name.endsWith(".class")
val JarEntry.simpleName get() = this.name.removeSuffix(".class")
val JarEntry.isManifest get() = this.name == "META-INF/MANIFEST.MF"

val JarFile.classLoader get() = File(this.name).classLoader
