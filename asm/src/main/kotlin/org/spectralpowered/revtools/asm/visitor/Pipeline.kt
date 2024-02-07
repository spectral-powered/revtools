/*
 * Copyright (C) 2024 Spectral Powered <https://github.com/spectral-powered>
 * @author Kyle Escobar <https://github.com/kyle-escobar>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.spectralpowered.revtools.asm.visitor

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.Package
import org.spectralpowered.revtools.asm.helper.collection.dequeOf
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.ir.Node

abstract class Pipeline(val group: ClassGroup, pipeline: List<NodeVisitor> = arrayListOf()) {
    protected open val pipeline = pipeline.map { it.wrap() }.toMutableList()

    operator fun plus(visitor: NodeVisitor) = add(visitor)
    operator fun plusAssign(visitor: NodeVisitor) {
        add(visitor)
    }

    open fun add(visitor: NodeVisitor) = pipeline.add(visitor.wrap())
    fun add(vararg visitors: NodeVisitor) {
        visitors.forEach { add(it) }
    }

    protected fun NodeVisitor.wrap(): ClassVisitor = when (val visitor = this) {
        is ClassVisitor -> visitor
        is MethodVisitor -> object : ClassVisitor {
            override val group get() = this@Pipeline.group

            override fun cleanup() {
                visitor.cleanup()
            }

            override fun visitMethod(method: Method) {
                super.visitMethod(method)
                visitor.visit(method)
            }
        }

        else -> object : ClassVisitor {
            override val group get() = this@Pipeline.group

            override fun cleanup() {
                visitor.cleanup()
            }

            override fun visit(node: Node) {
                super.visit(node)
                visitor.visit(node)
            }
        }
    }

    operator fun NodeVisitor.unaryPlus() {
        add(this)
    }

    abstract fun run()
}

class PackagePipeline(
    group: ClassGroup,
    val target: Package,
    pipeline: List<NodeVisitor> = arrayListOf()
) : Pipeline(group, pipeline) {
    override fun run() {
        val classes = group.getByPackage(target)
        for (pass in pipeline) {
            for (`class` in classes) {
                pass.visit(`class`)
            }
        }
    }
}

class MultiplePackagePipeline(
    group: ClassGroup,
    private val targets: List<Package>,
    pipeline: List<NodeVisitor> = arrayListOf()
) : Pipeline(group, pipeline) {
    override fun run() {
        val classes = targets.flatMap { group.getByPackage(it) }
        for (pass in pipeline) {
            for (`class` in classes) {
                pass.visit(`class`)
            }
        }
    }
}

class ClassPipeline(
    group: ClassGroup,
    target: Class,
    pipeline: List<NodeVisitor> = arrayListOf()
) : Pipeline(group, pipeline) {
    private val targets = mutableSetOf<Class>()

    init {
        val classQueue = dequeOf(target)
        while (classQueue.isNotEmpty()) {
            val top = classQueue.pollFirst()
            targets += top
            classQueue.addAll(top.innerClasses.keys.filterNot { it in targets })
        }
    }

    override fun run() {
        for (pass in pipeline) {
            for (`class` in targets) {
                pass.visit(`class`)
            }
        }
    }
}

open class MethodPipeline(
    group: ClassGroup,
    val targets: Collection<Method>,
    pipeline: List<NodeVisitor> = arrayListOf()
) : Pipeline(group, pipeline) {
    private val classTargets = targets.map { it.klass }.toMutableSet()
    override val pipeline = pipeline.map { it.methodWrap() }.toMutableList()

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun NodeVisitor.methodWrap(): ClassVisitor = when (val visitor = this) {
        is ClassVisitor -> object : ClassVisitor {
            override val group get() = this@MethodPipeline.group

            override fun cleanup() {
                visitor.cleanup()
            }

            override fun visit(klass: Class) {
                super.visit(klass)
                visitor.visit(klass)
            }

            override fun visitMethod(method: Method) {
                if (method in targets) {
                    super.visitMethod(method)
                    visitor.visitMethod(method)
                }
            }
        }

        is MethodVisitor -> object : ClassVisitor {
            override val group get() = this@MethodPipeline.group

            override fun cleanup() {
                visitor.cleanup()
            }

            override fun visitMethod(method: Method) {
                if (method in targets) {
                    super.visitMethod(method)
                    visitor.visit(method)
                }
            }
        }

        else -> this.wrap()
    }

    override fun add(visitor: NodeVisitor) = pipeline.add(visitor.methodWrap())

    override fun run() {
        for (pass in pipeline) {
            for (`class` in classTargets) {
                pass.visit(`class`)
            }
        }
    }
}

fun buildPipeline(group: ClassGroup, target: Package, init: Pipeline.() -> Unit): Pipeline =
    PackagePipeline(group, target).also {
        it.init()
    }

fun buildPipeline(group: ClassGroup, targets: List<Package>, init: Pipeline.() -> Unit): Pipeline =
    MultiplePackagePipeline(group, targets).also {
        it.init()
    }

fun buildPipeline(group: ClassGroup, target: Class, init: Pipeline.() -> Unit): Pipeline =
    ClassPipeline(group, target).also {
        it.init()
    }

fun buildPipeline(group: ClassGroup, targets: Collection<Method>, init: Pipeline.() -> Unit): Pipeline =
    MethodPipeline(group, targets).also {
        it.init()
    }

fun executePipeline(group: ClassGroup, target: Package, init: Pipeline.() -> Unit) =
    buildPipeline(group, target, init).run()

@Suppress("unused")
fun executePipeline(group: ClassGroup, targets: List<Package>, init: Pipeline.() -> Unit) =
    buildPipeline(group, targets, init).run()

fun executePipeline(group: ClassGroup, target: Class, init: Pipeline.() -> Unit) =
    buildPipeline(group, target, init).run()

fun executePipeline(group: ClassGroup, targets: Collection<Method>, init: Pipeline.() -> Unit) =
    buildPipeline(group, targets, init).run()
