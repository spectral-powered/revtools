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

package org.spectralpowered.revtools.asm.ir

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.AsmException
import org.spectralpowered.revtools.asm.builder.cfg.CfgBuilder
import org.spectralpowered.revtools.asm.ir.value.BlockUsageContext
import org.spectralpowered.revtools.asm.ir.value.BlockUser
import org.spectralpowered.revtools.asm.ir.value.SlotTracker
import org.spectralpowered.revtools.asm.ir.value.UsableBlock
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.type.TypeFactory
import org.spectralpowered.revtools.asm.type.parseMethodDesc
import org.spectralpowered.revtools.asm.util.jsrInlined
import org.spectralpowered.revtools.asm.helper.KtException
import org.spectralpowered.revtools.asm.helper.assert.ktassert
import org.spectralpowered.revtools.asm.helper.collection.queueOf
import org.spectralpowered.revtools.asm.helper.graph.GraphView
import org.spectralpowered.revtools.asm.helper.graph.PredecessorGraph
import org.spectralpowered.revtools.asm.helper.graph.Viewable

data class MethodDescriptor(
    val args: List<Type>,
    val returnType: Type
) {
    private val hash = args.hashCode() * 31 + returnType.hashCode()

    companion object {
        fun fromDesc(tf: TypeFactory, desc: String): MethodDescriptor {
            val (argTypes, returnType) = parseMethodDesc(tf, desc)
            return MethodDescriptor(argTypes, returnType)
        }
    }

    val asmDesc: String
        get() = "(${args.joinToString(separator = "") { it.asmDesc }})${returnType.asmDesc}"

    override fun hashCode() = hash
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as MethodDescriptor
        return this.args == other.args && this.returnType == other.returnType
    }

    override fun toString() = "(${args.joinToString { it.name }}): ${returnType.name}"
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class MethodBody(val method: Method) : PredecessorGraph<BasicBlock>, Iterable<BasicBlock>, BlockUser, Viewable {
    private val innerBlocks = arrayListOf<BasicBlock>()
    private val innerCatches = hashSetOf<CatchBlock>()
    val basicBlocks: List<BasicBlock> get() = innerBlocks
    val catchEntries: Set<CatchBlock> get() = innerCatches
    val slotTracker = SlotTracker(this)

    override val entry: BasicBlock
        get() = innerBlocks.first { it is BodyBlock && it.predecessors.isEmpty() }

    val bodyBlocks: List<BasicBlock>
        get() {
            val catches = catchBlocks
            return innerBlocks.filter { it !in catches }
        }

    val catchBlocks: List<BasicBlock>
        get() {
            val catchMap = hashMapOf<BasicBlock, Boolean>()
            val visited = hashSetOf<BasicBlock>()
            val result = arrayListOf<BasicBlock>()
            val queue = queueOf<BasicBlock>()
            queue.addAll(catchEntries)
            while (queue.isNotEmpty()) {
                val top = queue.poll()
                val isCatch = top.predecessors.fold(true) { acc, bb -> acc && catchMap.getOrPut(bb) { false } }
                if (isCatch && top !in visited) {
                    result.add(top)
                    queue.addAll(top.successors)
                    catchMap[top] = true
                    visited += top
                }
            }
            return result
        }

    override val nodes: Set<BasicBlock>
        get() = innerBlocks.toSet()

    fun isEmpty() = innerBlocks.isEmpty()
    fun isNotEmpty() = !isEmpty()

    internal fun clear() {
        innerBlocks.clear()
        innerCatches.clear()
    }

    fun add(ctx: BlockUsageContext, bb: BasicBlock) = with(ctx) {
        if (bb !in innerBlocks) {
            ktassert(!bb.hasParent, "Block ${bb.name} already belongs to other method")
            innerBlocks.add(bb)
            slotTracker.addBlock(bb)
            bb.addUser(this@MethodBody)
            bb.parentUnsafe = this@MethodBody
        }
    }

    fun addBefore(ctx: BlockUsageContext, before: BasicBlock, bb: BasicBlock) = with(ctx) {
        if (bb !in innerBlocks) {
            ktassert(!bb.hasParent, "Block ${bb.name} already belongs to other method")
            val index = basicBlocks.indexOf(before)
            ktassert(index >= 0, "Block ${before.name} does not belong to method ${method.prototype}")

            innerBlocks.add(index, bb)
            slotTracker.addBlock(bb)
            bb.addUser(this@MethodBody)
            bb.parentUnsafe = this@MethodBody
        }
    }

    fun addAfter(ctx: BlockUsageContext, after: BasicBlock, bb: BasicBlock) = with(ctx) {
        if (bb !in innerBlocks) {
            ktassert(!bb.hasParent, "Block ${bb.name} already belongs to other method")
            val index = basicBlocks.indexOf(after)
            ktassert(index >= 0, "Block ${after.name} does not belong to method ${method.prototype}")

            innerBlocks.add(index + 1, bb)
            slotTracker.addBlock(bb)
            bb.addUser(this@MethodBody)
            bb.parentUnsafe = this@MethodBody
        }
    }

    fun remove(ctx: BlockUsageContext, block: BasicBlock) = with(ctx) {
        if (innerBlocks.contains(block)) {
            ktassert(block.parentUnsafe == this@MethodBody, "Block ${block.name} don't belong to ${method.prototype}")
            innerBlocks.remove(block)

            if (block in innerCatches) {
                innerCatches.remove(block)
            }

            block.removeUser(this@MethodBody)
            block.parentUnsafe = null
            slotTracker.removeBlock(block)
        }
    }

    fun addCatchBlock(bb: CatchBlock) {
        require(bb in innerBlocks)
        innerCatches.add(bb)
    }

    fun getNext(from: BasicBlock): BasicBlock {
        val start = innerBlocks.indexOf(from)
        return innerBlocks[start + 1]
    }

    fun getBlockByLocation(location: Location) = innerBlocks.find { it.location == location }
    fun getBlockByName(name: String) = innerBlocks.find { it.name.toString() == name }

    fun print() = buildString {
        append(innerBlocks.joinToString(separator = "\n\n") { "$it" })
    }

    override fun toString() = print()
    override fun iterator() = innerBlocks.iterator()

    override fun hashCode() = method.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as MethodBody
        return this.method == other.method
    }

    override fun replaceUsesOf(ctx: BlockUsageContext, from: UsableBlock, to: UsableBlock) = with(ctx) {
        for (index in innerBlocks.indices) {
            if (basicBlocks[index] == from) {
                innerBlocks[index].removeUser(this@MethodBody)
                innerBlocks[index] = to.get()
                to.addUser(this@MethodBody)
            }
        }
    }

    override fun clearBlockUses(ctx: BlockUsageContext) = with(ctx) {
        innerBlocks.forEach {
            it.removeUser(this@MethodBody)
        }
    }

    override val graphView: List<GraphView>
        get() {
            val nodes = hashMapOf<String, GraphView>()
            nodes[method.name] = GraphView(method.name, method.prototype)

            for (bb in basicBlocks) {
                val label = StringBuilder()
                label.append("${bb.name}: ${bb.predecessors.joinToString(", ") { it.name.toString() }}\\l")
                bb.instructions.forEach { label.append("    ${it.print().replace("\"", "\\\"")}\\l") }
                nodes[bb.name.toString()] = GraphView(bb.name.toString(), label.toString())
            }

            if (!method.isAbstract) {
                val entryNode = nodes.getValue(entry.name.toString())
                nodes.getValue(method.name).addSuccessor(entryNode)
            }

            for (it in basicBlocks) {
                val current = nodes.getValue(it.name.toString())
                for (successor in it.successors) {
                    current.addSuccessor(nodes.getValue(successor.name.toString()))
                }
            }

            return nodes.values.toList()
        }
}

@Suppress("unused")
class Method : Node {
    val klass: Class
    internal val mn: MethodNode
    var bodyInitialized: Boolean = false
        private set

    val body: MethodBody by lazy {
        bodyInitialized = true
        try {
            if (!isAbstract) CfgBuilder(group, this).build()
            else MethodBody(this)
        } catch (e: AsmException) {
            if (group.failOnError) throw e
            MethodBody(this)
        } catch (e: KtException) {
            if (group.failOnError) throw e
            MethodBody(this)
        }
    }

    // we need this suppresses, because when setter
    // is called from constructor field is actually null
    private var descInternal: MethodDescriptor? = null
        set(value) {
            field?.let { klass.updateMethod(it, value!!, this) }
            field = value
        }

    var desc: MethodDescriptor
        get() = descInternal!!
        set(value) {
            descInternal = value
        }
    var parameters = listOf<Parameter>()
    var exceptions = setOf<Class>()

    companion object {
        const val CONSTRUCTOR_NAME = "<init>"
        const val STATIC_INIT_NAME = "<clinit>"
    }

    constructor(
        group: ClassGroup,
        klass: Class,
        node: MethodNode
    ) : super(group, node.name, Modifiers(node.access)) {
        this.klass = klass
        this.mn = node.jsrInlined
        this.desc = MethodDescriptor.fromDesc(group.type, node.desc)
        this.parameters = getParameters(mn)
        this.exceptions = mn.exceptions.mapTo(mutableSetOf()) { group[it] }
    }

    constructor(
        group: ClassGroup,
        klass: Class,
        name: String,
        desc: MethodDescriptor,
        modifiers: Modifiers = Modifiers(0)
    ) : super(group, name, modifiers) {
        this.klass = klass
        this.mn = MethodNode(modifiers.value, name, desc.asmDesc, null, null)
        this.desc = desc
    }

    private fun getParameters(methodNode: MethodNode): List<Parameter> {
        return when {
            mn.parameters.isNotEmpty() -> processNodeParameters(methodNode)
            else -> processImplicitNodeParameters(methodNode)
        }
    }

    private fun processNodeParameters(methodNode: MethodNode): List<Parameter> = buildList {
        val invisibleParameterAnnotations = methodNode.invisibleParameterAnnotations ?: arrayOfNulls(mn.parameters.size)
        val visibleParameterAnnotations = methodNode.visibleParameterAnnotations ?: arrayOfNulls(mn.parameters.size)

        for ((index, param) in mn.parameters.withIndex()) {
            val parameterAnnotationsNodes = invisibleParameterAnnotations[index].orEmpty() +
                    visibleParameterAnnotations[index].orEmpty()

            val annotations = parameterAnnotationsNodes.map { annotationNode ->
                MethodParameterAnnotation.get(annotationNode, group)
            }

            add(Parameter(group, index, param?.name ?: "", desc.args[index], Modifiers(param?.access ?: 0), annotations))
        }
    }

    private fun processImplicitNodeParameters(methodNode: MethodNode): List<Parameter> = buildList {
        val invisibleParameterAnnotations = methodNode.invisibleParameterAnnotations
        val visibleParameterAnnotations = methodNode.visibleParameterAnnotations
        invisibleParameterAnnotations?.let { annotations ->
            addAll(getParametersStubs(annotations))
        }

        visibleParameterAnnotations?.let { annotations ->
            addAll(getParametersStubs(annotations))
        }
    }

    private fun getParametersStubs(annotations: Array<List<AnnotationNode>>): List<Parameter> {
        return annotations.mapIndexed { index, parameterAnnotations ->
            val type = group.type.voidType

            @Suppress("UselessCallOnNotNull")  // interoperability error: parameterAnnotations may be null
            val annotationsOfParameter = parameterAnnotations.orEmpty().map { annotationNode ->
                MethodParameterAnnotation.get(annotationNode, group)
            }
            StubParameter(group, index, type, Modifiers(0), annotationsOfParameter)
        }
    }

    val argTypes get() = desc.args
    val returnType get() = desc.returnType

    val prototype: String
        get() = "$klass::$name$desc"

    val isConstructor: Boolean
        get() = name == CONSTRUCTOR_NAME

    val isStaticInitializer: Boolean
        get() = name == STATIC_INIT_NAME

    override val asmDesc
        get() = desc.asmDesc

    val hasBody get() = body.isNotEmpty()
    val hasLoops get() = group.loopManager.getMethodLoopInfo(this).isNotEmpty()

    fun getLoopInfo() = group.loopManager.getMethodLoopInfo(this)
    fun invalidateLoopInfo() = group.loopManager.setInvalid(this)

    fun print() = buildString {
        appendLine(prototype)
        if (bodyInitialized) append(body.print())
    }

    override fun toString() = prototype

//    override fun hashCode() = defaultHashCode(name, klass, desc)

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + klass.hashCode()
        result = 31 * result + desc.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as Method
        return this.name == other.name && this.klass == other.klass && this.desc == other.desc
    }

    fun view(dot: String, viewer: String) {
        body.view(name, dot, viewer)
    }
}
