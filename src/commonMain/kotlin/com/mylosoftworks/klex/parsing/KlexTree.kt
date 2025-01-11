package com.mylosoftworks.klex.parsing


/**
 * The KlexTree is a tree/tree item containing all parsed definitions in structure. Intended to be converted to another tree structure, serving as an AST.
 *
 * @param T The type to convert to when converting.
 */
data class KlexTree<T>(val strContent: String, val value: T?, var children: List<KlexTree<T>>) {
    /**
     * Use this function to finalize
     */
    fun <Tree> convert(transform: (strContent: String, value: T?, children: List<Tree>) -> Tree): Tree {
        return transform(strContent, value, children.map { it.convert(transform) })
    }

    fun flattenNullValues(): List<KlexTree<T>> {
        val newChildren = children.flatMap { it.flattenNullValues() }
        return if (value == null) newChildren else listOf(KlexTree(strContent, value, newChildren))
    }

    fun find(includeSelf: Boolean = false, deep: Boolean = true, predicate: (KlexTree<T>) -> Boolean): KlexTree<T>? {
        if (includeSelf && predicate(this)) return this

        if (!deep) {
            return children.find(predicate)
        }

        children.forEach {
            val result = it.find(true, true, predicate)
            if (result != null) return result
        }
        return null
    }

    fun findAll(includeSelf: Boolean = false, deep: Boolean = true, predicate: (KlexTree<T>) -> Boolean): List<KlexTree<T>> {
        val list = mutableListOf<KlexTree<T>>()

        if (includeSelf && predicate(this)) list.add(this)

        if (!deep) {
            list.addAll(children.filter(predicate))
            return list
        }

        children.forEach {
            val results = it.findAll(true, true, predicate)
            list.addAll(results)
        }

        return list
    }
}