import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.cls
import org.spectralpowered.revtools.deobfuscator.asm.tree.removeDeadCode

class ControlFlowOptimizer : Transformer {

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                method.removeDeadCode()
                val cfg = ControlFlowGraph(method)
                val blocks = cfg.blocks
                println()
            }
        }
    }

    class ControlFlowGraph(private val method: MethodNode) : Analyzer<BasicValue>(BasicInterpreter()) {

        val blocks = mutableListOf<Block>()
        val catchBlocks = mutableListOf<CatchBlock>()

        private val insnToBlock = MutableList<Block?>(method.instructions.size()) { null }
        private val blockToInsn = hashMapOf<Block, MutableList<AbstractInsnNode>>()
        private val insnIndicies = hashMapOf<AbstractInsnNode, Int>()

        private fun addBlock(block: Block) {
            if(block !in blocks) {
                blocks.add(block)
            }
        }

        private fun addCatchBlock(block: CatchBlock) {
            require(block in blocks)
            catchBlocks.add(block)
        }

        init {
            analyze(method.cls.name, method)
        }

        private val AbstractInsnNode.index: Int get() = insnIndicies.getOrPut(this) { method.instructions.indexOf(this) }

        private inline fun <T> MutableList<T?>.getOrSet(index: Int, block: () -> T): T {
            if(this[index] == null) {
                this[index] = block()
            }
            return this[index]!!
        }

        override fun init(owner: String, method: MethodNode) {
            for(tcb in method.tryCatchBlocks) {
                val type = when(tcb.type) {
                    null -> CatchBlock.DEFAULT_EXCEPTION_TYPE
                    else -> Type.getType(tcb.type)
                }
                insnToBlock[tcb.handler.index] = CatchBlock("catch", type)
            }

            var block: Block = BasicBlock("block")
            var insnList = blockToInsn.getOrPut(block, ::arrayListOf)

            for((insnIndex, insn) in method.instructions.withIndex()) {
                if(insn is LabelNode) {
                    when {
                        insn.next == null -> Unit
                        insn.previous == null -> {
                            block = insnToBlock.getOrSet(insnIndex) { block }

                            val entry = BasicBlock("entry")
                            blockToInsn[entry] = arrayListOf()
                            entry.linkForward(block)

                            addBlock(entry)
                        }
                        else -> {
                            block = insnToBlock.getOrSet(insnIndex) { BasicBlock("label") }
                            insnList = blockToInsn.getOrPut(block, ::arrayListOf)

                            if(!insn.previous.isTerminator()) {
                                val prev = insnToBlock[insnIndex - 1]!!
                                block.linkBackward(prev)
                            }
                        }
                    }
                } else {
                    block = insnToBlock.getOrSet(insnIndex) { block }
                    insnList = blockToInsn.getOrPut(block, ::arrayListOf)

                    when(insn) {
                        is JumpInsnNode -> {
                            if(insn.opcode != GOTO) {
                                val falseSuccessor = insnToBlock.getOrSet(insnIndex + 1) { BasicBlock("if.else") }
                                block.linkForward(falseSuccessor)
                            }
                            val trueSuccessorName = if(insn.opcode == GOTO) "goto" else "if.then"
                            val trueSuccessor = insnToBlock.getOrSet(insn.label.index) { BasicBlock(trueSuccessorName) }
                            block.linkForward(trueSuccessor)
                        }

                        is TableSwitchInsnNode -> {
                            val defaultBlock = insnToBlock.getOrSet(insn.dflt.index) { BasicBlock("tableswitch.default") }
                            block.linkForward(defaultBlock)

                            val labels = insn.labels
                            for(label in labels) {
                                val labelBlock = insnToBlock.getOrSet(label.index) { BasicBlock("tableswitch") }
                                block.linkForward(labelBlock)
                            }
                        }

                        is LookupSwitchInsnNode -> {
                            val defaultBlock = insnToBlock.getOrSet(insn.dflt.index) { BasicBlock("lookupswitch.default") }
                            block.linkForward(defaultBlock)

                            val labels = insn.labels
                            for(label in labels) {
                                val labelBlock = insnToBlock.getOrSet(label.index) { BasicBlock("lookupswitch") }
                                block.linkForward(labelBlock)
                            }
                        }

                        else -> {
                            if(insn.canThrowException() && (insn.next != null)) {
                                val next = insnToBlock.getOrSet(insnIndex + 1) { BasicBlock("block") }
                                if(!insn.isTerminator()) {
                                    block.linkForward(next)
                                }
                            }
                        }
                    }
                }
                insnList.add(insn)
                addBlock(block)
            }

            for(insn in method.tryCatchBlocks) {
                val handlerIndex = insn.handler.index
                val handler = insnToBlock[handlerIndex] as CatchBlock
                var curIdx: Int = insn.start.index

                var thrower = insnToBlock[curIdx]!!
                val throwers = arrayListOf<Block>()
                while(method.instructions[curIdx] != insn.end) {
                    block = insnToBlock[curIdx]!!
                    if(block.name != thrower.name) {
                        throwers.add(thrower)
                        thrower.addHandler(handler)
                        thrower = block
                    }
                    curIdx++
                }

                if(thrower !in throwers) {
                    throwers.add(thrower)
                    thrower.addHandler(handler)
                }
                handler.addThrowers(throwers)
                addCatchBlock(handler)
            }
        }

        private fun AbstractInsnNode.isTerminator(): Boolean = when(opcode) {
            TABLESWITCH, LOOKUPSWITCH, GOTO, ATHROW -> true
            in IRETURN..RETURN -> true
            else -> false
        }

        private fun AbstractInsnNode.canThrowException(): Boolean = when(opcode) {
            in NOP..ALOAD -> false
            in IALOAD..SALOAD -> true
            in ISTORE..ASTORE -> false
            in IASTORE..SASTORE -> true
            in POP..DMUL -> false
            in IDIV..DREM -> true
            in INEG..PUTSTATIC -> false
            in GETFIELD..INVOKEDYNAMIC -> true
            NEW -> false
            in NEWARRAY..CHECKCAST -> true
            INSTANCEOF -> false
            in MONITORENTER..MULTIANEWARRAY -> true
            in IFNULL..IFNONNULL -> false
            -1 -> false
            else -> throw IllegalArgumentException("Unknown instruction opcode $opcode.")
        }
    }

    abstract class Block(val name: String) {

        val predecessors = linkedSetOf<Block>()
        val successors = linkedSetOf<Block>()
        val handlers = hashSetOf<CatchBlock>()
        val instructions = arrayListOf<AbstractInsnNode>()

        fun addSuccessor(block: Block) {
            successors.add(block)
        }

        fun addPredecessor(block: Block) {
            predecessors.add(block)
        }

        fun addHandler(handle: CatchBlock) {
            handlers.add(handle)
        }

        fun linkForward(block: Block) {
            val current = this
            current.addSuccessor(block)
            block.addPredecessor(current)
        }

        fun linkBackward(block: Block) {
            val current = this
            current.addPredecessor(block)
            block.addSuccessor(current)
        }

        fun linkThrowing(block: CatchBlock) {
            val current = this
            current.addHandler(block)
            block.addThrower(current)
        }
    }

    class BasicBlock(name: String) : Block(name) {

    }

    class CatchBlock(name: String, val type: Type = DEFAULT_EXCEPTION_TYPE) : Block(name) {

        val throwers = hashSetOf<Block>()

        fun addThrower(thrower: Block) {
            throwers.add(thrower)
        }

        fun addThrowers(throwers: List<Block>) {
            throwers.forEach { addThrower(it) }
        }

        fun linkCatching(thrower: Block) {
            val current = this
            current.addThrower(thrower)
            thrower.addHandler(current)
        }

        companion object {
            val DEFAULT_EXCEPTION_TYPE = Type.getType(Throwable::class.java)
        }
    }

}