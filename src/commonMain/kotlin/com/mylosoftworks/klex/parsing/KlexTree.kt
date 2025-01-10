package com.mylosoftworks.klex.parsing


/**
 * The KlexTree is a tree/tree item containing all parsed definitions in structure. Intended to be converted to another tree structure, serving as an AST.
 *
 * @param T The type to convert to when converting.
 */
class KlexTree<T>(val strContent: String, val value: T?, var children: List<KlexTree<T>>) {
    /**
     * Use this function to finalize
     */
    fun <Tree> convert(transform: (strContent: String, value: T?, children: List<Tree>) -> Tree): Tree {
        return transform(strContent, value, children.map { it.convert(transform) })
    }

    fun flattenNullValues(): List<KlexTree<T>> {
        children = children.flatMap { it.flattenNullValues() }
        return if (value == null) children else listOf(this)
    }
}