package com.mylosoftworks.klex.parsing


/**
 * The KlexTree is a tree/tree item containing all parsed definitions in structure. Intended to be converted to another tree structure, serving as an AST.
 *
 * @param T The type to convert to when converting.
 */
class KlexTree<T>(val strContent: String, val value: T?, var children: List<KlexTree<T>>) {
    /**
     * Use this function to convert to your custom tree structure
     */
    fun <Tree> convert(transform: (strContent: String, value: T?, children: List<Tree>) -> Tree): Tree {
        return transform(strContent, value, children.map { it.convert(transform) })
    }

    /**
     * Use this function to remove all tree elements without a value, and merge them upwards
     */
    fun flattenNullValues(): List<KlexTree<T>> {
        val newChildren = children.flatMap { it.flattenNullValues() }
        return if (value == null) newChildren else listOf(KlexTree(strContent, value, newChildren))
    }

    /**
     * Find the first tree item which matches the predicate, or null if no match was found.
     */
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

    /**
     * Find all tree items which match the predicate.
     */
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

    override fun toString(): String {
        return "KlexTree(strContent='$strContent', value=$value, children=$children)"
    }

    operator fun get(index: Int) = children[index]
    operator fun component1() = get(0)
    operator fun component2() = get(1)
    operator fun component3() = get(2)
    operator fun component4() = get(3)
    operator fun component5() = get(4)
    operator fun component6() = get(5)
    operator fun component7() = get(6)
    operator fun component8() = get(7)
    operator fun component9() = get(8)
    operator fun component10() = get(9)
}