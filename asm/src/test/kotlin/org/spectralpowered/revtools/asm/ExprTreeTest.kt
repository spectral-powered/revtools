package org.spectralpowered.revtools.asm

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeSameInstanceAs

class ExprTreeTest : FunSpec({

    extension(AsmTest)
    val group = AsmTest.group

    test("Expr Tree") {
        val cls = group.findClass("testclasses/Test1")!!
        val method = cls.findMethod("test", "()V")!!
        val insnsA = method.instructions.toArray()
        val insnsB = method.exprTree.instructions.toArray()
        for(i in insnsA.indices) {
            val insnA = insnsA[i]
            val insnB = insnsB[i]
            insnA.opcode shouldBeEqual insnB.opcode
        }
    }
})