package com.mylosoftworks.klex.parsing


/**
 * The KlexTree is a tree/tree item containing all parsed definitions in structure. Intended to be converted to another tree structure, serving as an AST.
 *
 * @param T The type to convert to when converting.
 */
open class KlexTree<T, Type>(val content: Type, val value: T?, var children: List<KlexTree<T, Type>>) {
    /**
     * Use this function to convert to your custom tree structure
     */
    fun <Tree> convert(transform: (strContent: Type, value: T?, children: List<Tree>) -> Tree): Tree {
        return transform(content, value, children.map { it.convert(transform) })
    }

    /**
     * Use this function to remove all tree elements without a value, and merge them upwards
     */
    fun flattenNullValues(): List<KlexTree<T, Type>> {
        val newChildren = children.flatMap { it.flattenNullValues() }
        return if (value == null) newChildren else listOf(KlexTree(content, value, newChildren))
    }

    /**
     * Find the first tree item which matches the predicate, or null if no match was found.
     */
    fun find(includeSelf: Boolean = false, deep: Boolean = true, predicate: (KlexTree<T, Type>) -> Boolean): KlexTree<T, Type>? {
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
    fun findAll(includeSelf: Boolean = false, deep: Boolean = true, predicate: (KlexTree<T, Type>) -> Boolean): List<KlexTree<T, Type>> {
        val list = mutableListOf<KlexTree<T, Type>>()

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

    /**
     * Gets the index of a descendant. Index is only incremented by deepest items. Index is not guaranteed to start at 0.
     */
    fun getIndexOfDescendant(item: KlexTree<T, Type>): Int {
        return getIndexOfDescendantPrivate(item).first
    }

    private fun getIndexOfDescendantPrivate(item: KlexTree<T, Type>, index: Int = -1): Triple<Int, Boolean, Boolean> {
        var index = index
        children.forEach {
            val (newIndex, increment, hit) = it.getIndexOfDescendantPrivate(item, index)
            index = newIndex
            if (hit) return Triple(index, increment, hit)
        }
        val noChildren = children.isEmpty()
        return Triple(if (noChildren) index + 1 else index, noChildren, this == item)
    }

    override fun toString(): String {
        return "KlexTree(strContent='$content', value=$value, children=$children)"
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