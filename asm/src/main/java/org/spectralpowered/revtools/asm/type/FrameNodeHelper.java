package org.spectralpowered.revtools.asm.type;

import org.objectweb.asm.tree.FrameNode;

class FrameNodeHelper {
    public static int getFrameType(FrameNode node) {
        return node.type;
    }
}
