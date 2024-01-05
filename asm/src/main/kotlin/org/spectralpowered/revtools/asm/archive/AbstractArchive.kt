package org.spectralpowered.revtools.asm.archive

import org.spectralpowered.revtools.asm.ClassGroup
import java.io.File

abstract class AbstractArchive(val file: File) {

    abstract fun read(group: ClassGroup)

    abstract fun write(group: ClassGroup, writeIgnoredClasses: Boolean, writeResources: Boolean)

}