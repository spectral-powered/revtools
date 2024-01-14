import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import org.jgrapht.Graphs
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.DepthFirstIterator
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.cls
import java.util.*
import kotlin.math.min


class ControlFlowOptimizer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        for(method in group.classes.flatMap { it.methods }) {
            if(method.tryCatchBlocks.isNotEmpty()) continue
            val cfg = ControlFlowGraph(method).build()
            val blocks = cfg.blocks
            if(blocks.isNotEmpty()) {
                val insns = method.instructions
                val newInsns = InsnList()

                val labels = hashMapOf<LabelNode, LabelNode>()
                insns.filterIsInstance<LabelNode>().forEachIndexed { _, label ->
                    labels[label] = LabelNode(label.label)
                }

                val queue = Stack<Block>()
                val visited = hashSetOf<Block>()
                queue.push(blocks.first())
                while(queue.isNotEmpty()) {
                    val block = queue.pop()
                    if(block in visited) continue
                    visited.add(block)
                    block.branches.forEach { queue.push(it.root) }
                    block.next?.also { queue.push(it) }
                    for(i in block.start until block.end) {
                        newInsns.add(insns[i].clone(labels))
                    }
                }
                method.instructions = newInsns
                count += blocks.size
            }
        }
    }

    private class ControlFlowGraph(private val method: MethodNode) : Analyzer<BasicValue>(BasicInterpreter()) {

        private val blockGraph = DefaultDirectedGraph<Block, DefaultEdge>(DefaultEdge::class.java)
        val blocks = mutableListOf<Block>()

        fun build(): ControlFlowGraph {
            analyze(method.cls.name, method)
            return this
        }

        override fun init(owner: String, method: MethodNode) {
            var block = Block()
            blocks.add(block)
            for((insnIndex, insn) in method.instructions.withIndex()) {
                block.end++
                if(insn.next == null) break
                if(insn.next is LabelNode || insn is JumpInsnNode || insn is TableSwitchInsnNode || insn is LookupSwitchInsnNode) {
                    block = Block()
                    block.start = insnIndex + 1
                    block.end = insnIndex + 1
                    blocks.add(block)
                }
            }
        }

        override fun newControlFlowEdge(from: Int, to: Int) {
            val fromBlock = blocks.first { from in it.start until it.end }
            val toBlock = blocks.first { to in it.start until it.end }
            blockGraph.addVertex(fromBlock)
            blockGraph.addVertex(toBlock)
            blockGraph.addEdge(fromBlock, toBlock)
            if(fromBlock != toBlock) {
                blockGraph.addEdge(fromBlock, toBlock)
                if(from + 1 == to) {
                    fromBlock.next = toBlock
                    toBlock.prev = fromBlock
                } else {
                    fromBlock.branches.add(toBlock)
                }
            }
        }

        override fun newControlFlowExceptionEdge(from: Int, to: Int): Boolean {
            //newControlFlowEdge(from, to)
            return true
        }
    }

    private class Block {

        var start = 0
        var end = 0
        var prev: Block? = null
        var next: Block? = null
        val branches = mutableListOf<Block>()

        val root: Block get() {
            var cur = this
            var last = prev
            while(last != null) {
                cur = last
                last = cur.prev
            }
            return cur
        }

        override fun toString(): String {
            return "Block(start=$start, end=$end)"
        }
    }

    /**
     * An implementation of the O(n log n) Lengauer-Tarjan algorithm for building the
     * [dominator tree](http://en.wikipedia.org/wiki/Dominator_%28graph_theory%29)
     * of a flowgraph.
     */
    class DominatorTree<V, E>(graph: DefaultDirectedGraph<V, E>, root: V) {
        private val graph: DefaultDirectedGraph<V, E>

        /**
         * Semidominator numbers by block.
         */
        private val semi: MutableMap<V?, Int> = HashMap()

        /**
         * Parents by block.
         */
        private val parent: MutableMap<V, V> = HashMap()

        /**
         * Predecessors by block.
         */
        private val pred: SetMultimap<V, V> = HashMultimap.create()

        /**
         * Blocks in DFS order; used to look up a block from its semidominator
         * numbering.
         */
        private val vertex = ArrayList<V>()

        /**
         * Blocks by semidominator block.
         */
        private val bucket: SetMultimap<V?, V> = HashMultimap.create()

        /**
         * idominator map, built iteratively.
         */
        private val idom: MutableMap<V, V> = HashMap()

        /**
         * Dominance frontiers of this dominator tree, built on demand.
         */
        var dominanceFrontiers: SetMultimap<V, V>? = null
            /**
             * Compute and/or fetch the dominance frontiers as a SetMultimap.
             *
             * @return a SetMultimap where the set of nodes mapped to each key
             * node is the set of nodes in the key node's dominance frontier.
             */
            get() {
                if (field == null) {
                    field = HashMultimap.create()
                    getDominatorTree() // touch the dominator tree
                    for (x in reverseTopologicalTraversal()) {
                        val dfx = field!!.get(x)

                        //  Compute DF(local)
                        for (y in getSuccessors(x)) {
                            if (idom[y] !== x) {
                                dfx.add(y)
                            }
                        }

                        //  Compute DF(up)
                        for (z in dominatorTree!![x]) {
                            for (y in field!!.get(z)) {
                                if (idom[y] !== x) {
                                    dfx.add(y)
                                }
                            }
                        }
                    }
                }
                return field
            }
            private set

        /**
         * Dominator tree, built on demand from the idominator map.
         */
        private var dominatorTree: SetMultimap<V, V>? = null

        /**
         * Auxiliary data structure used by the O(m log n) eval/link implementation:
         * ancestor relationships in the forest (the processed tree as it's built
         * back up).
         */
        private val ancestor: MutableMap<V, V> = HashMap()

        /**
         * Auxiliary data structure used by the O(m log n) eval/link implementation:
         * node with least semidominator seen during traversal of a path from node
         * to subtree root in the forest.
         */
        private val label: MutableMap<V?, V?> = HashMap()

        /**
         * A topological traversal of the dominator tree, built on demand.
         */
        private var topologicalTraversalImpl: LinkedList<V>? = null

        init {
            this.graph = graph
            dfs(root)
            computeDominators()
        }

        val idoms: Map<V, V>
            /**
             * Create and/or fetch the map of immediate dominators.
             *
             * @return the map from each block to its immediate dominator
             * (if it has one).
             */
            get() = idom

        /**
         * Compute and/or fetch the dominator tree as a SetMultimap.
         *
         * @return the dominator tree.
         */
        fun getDominatorTree(): SetMultimap<V, V> {
            if (dominatorTree == null) {
                dominatorTree = HashMultimap.create<V, V>()
                for (node in idom.keys) {
                    dominatorTree!!.get(idom[node]).add(node)
                }
            }
            return dominatorTree!!
        }

        private fun getSuccessors(v: V): List<V> {
            return Graphs.successorListOf(graph, v)
        }

        /**
         * Create and/or fetch a topological traversal of the dominator tree,
         * such that for every node, idom(node) appears before node.
         *
         * @return the topological traversal of the dominator tree,
         * as an immutable List.
         */
        fun topologicalTraversal(): List<V> {
            return Collections.unmodifiableList(toplogicalTraversalImplementation)
        }

        /**
         * Create and/or fetch a reverse topological traversal of the dominator tree,
         * such that for every node, node appears before idom(node).
         *
         * @return a reverse topological traversal of the dominator tree,
         * as an immutable List.
         */
        fun reverseTopologicalTraversal(): Iterable<V> {
            return object : Iterable<V> {
                override fun iterator(): Iterator<V> {
                    return toplogicalTraversalImplementation.descendingIterator()
                }
            }
        }

        private fun dfs(root: V) {
            val it = DepthFirstIterator<V, E>(graph, root)
            while (it.hasNext()) {
                val node = it.next()
                if (!semi.containsKey(node)) {
                    vertex.add(node)

                    //  Initial assumption: the node's semidominator is itself.
                    semi[node] = semi.size
                    label[node] = node
                    for (child in getSuccessors(node)) {
                        pred[child].add(node)
                        if (!semi.containsKey(child)) {
                            parent[child] = node
                        }
                    }
                }
            }
        }

        /**
         * Steps 2, 3, and 4 of Lengauer-Tarjan.
         */
        private fun computeDominators() {
            val lastSemiNumber = semi.size - 1
            for (i in lastSemiNumber downTo 1) {
                val w = vertex[i]
                val p = parent[w]!!

                //  step 2: compute semidominators
                //  for each v in pred(w)...
                var semidominator = semi[w]!!
                for (v in pred[w]) {
                    semidominator = min(semidominator.toDouble(), semi[eval(v)]!!.toDouble()).toInt()
                }
                semi[w] = semidominator
                bucket[vertex[semidominator]].add(w)

                //  Link w into the forest via its parent, p
                link(p, w)

                //  step 3: implicitly compute idominators
                //  for each v in bucket(parent(w)) ...
                for (v in bucket[p]) {
                    val u = eval(v)
                    if (semi[u]!! < semi[v]!!) {
                        idom[v] = u!!
                    } else {
                        idom[v] = p!!
                    }
                }
                bucket[p].clear()
            }

            // step 4: explicitly compute idominators
            for (i in 1..lastSemiNumber) {
                val w = vertex[i]
                if (idom[w] !== vertex[semi[w]!!]) {
                    idom[w] = idom[idom[w]]!!
                }
            }
        }

        /**
         * Extract the node with the least-numbered semidominator in the (processed)
         * ancestors of the given node.
         *
         * @param v - the node of interest.
         * @return "If v is the root of a tree in the forest, return v. Otherwise,
         * let r be the root of the tree which contains v. Return any vertex u != r
         * of miniumum semi(u) on the path r-*v."
         */
        private fun eval(v: V): V? {
            //  This version of Lengauer-Tarjan implements
            //  eval(v) as a path-compression procedure.
            compress(v)
            return label[v]
        }

        /**
         * Traverse ancestor pointers back to a subtree root, then propagate the
         * least semidominator seen along this path through the "label" map.
         */
        private fun compress(v: V) {
            val worklist = Stack<V?>()
            worklist.add(v)
            var a = ancestor[v]

            //  Traverse back to the subtree root.
            while (ancestor.containsKey(a)) {
                worklist.push(a)
                a = ancestor[a]
            }

            //  Propagate semidominator information forward.
            var ancestor = worklist.pop()
            var leastSemi = semi[label[ancestor]]!!
            while (!worklist.empty()) {
                val descendent = worklist.pop()
                val currentSemi = semi[label[descendent]]!!
                if (currentSemi > leastSemi) {
                    label[descendent] = label[ancestor]
                } else {
                    leastSemi = currentSemi
                }

                //  Prepare to process the next iteration.
                ancestor = descendent
            }
        }

        /**
         * Simple version of link(parent,child) simply links the child into the
         * parent's forest, with no attempt to balance the subtrees or otherwise
         * optimize searching.
         */
        private fun link(parent: V, child: V) {
            ancestor[child] = parent
        }

        private val toplogicalTraversalImplementation: LinkedList<V>
            /**
             * Create/fetch the topological traversal of the dominator tree.
             *
             * @return [this.topologicalTraversal], the traversal of
             * the dominator tree such that for any node n with a dominator,
             * n appears before idom(n).
             */
            private get() {
                if (topologicalTraversalImpl == null) {
                    topologicalTraversalImpl = LinkedList()
                    for (node in vertex) {
                        val idx = topologicalTraversalImpl!!.indexOf(idom[node])
                        if (idx != -1) {
                            topologicalTraversalImpl!!.add(idx + 1, node)
                        } else {
                            topologicalTraversalImpl!!.add(node)
                        }
                    }
                }
                return topologicalTraversalImpl!!
            }
    }

}