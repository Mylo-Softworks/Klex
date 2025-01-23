package com.mylosoftworks.klex.parsing

class KlexStringTree<T>(content: String, value: T?, children: List<KlexStringTree<T>>, val sourceStartIndex: Int, val sourceEndIndex: Int): AbstractKlexTree<T, String, KlexStringTree<T>>(content, value, children) {
    override fun copyWithNewChildren(newChildren: List<KlexStringTree<T>>): KlexStringTree<T> = KlexStringTree(content, value, newChildren, sourceStartIndex, sourceEndIndex)

    override fun toString(): String {
        return "KlexTree(start=$sourceStartIndex, end=$sourceEndIndex, strContent='$content', value=$value, children=$children)"
    }

    /**
     * Find the deepest descendant at the specified index.
     */
    fun findByIndex(index: Int): KlexStringTree<T>? {
        children.forEach {
            val result = it.findByIndex(index)
            if (result != null) return result
        }
        // None of the children matched or we have no children, now check if this matches
        val thisMatches = (sourceStartIndex..<sourceEndIndex).contains(index)
        return if (thisMatches) this else null
    }
}