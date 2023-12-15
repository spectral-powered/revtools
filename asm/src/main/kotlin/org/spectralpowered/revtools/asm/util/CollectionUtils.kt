package org.spectralpowered.revtools.asm.util

inline fun <A, K, V, R : MutableMap<K, V>> Collection<A>.mapIndexedNotNullTo(
    result: R,
    body: (Int, A) -> Pair<K, V>?
): R {
    for (element in this.withIndex()) {
        val transformed = body(element.index, element.value) ?: continue
        result += transformed
    }
    return result
}