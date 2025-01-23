package com.mylosoftworks.klex.parsing

class KlexTree<T, Type>(content: Type, value: T?, children: List<KlexTree<T, Type>>): AbstractKlexTree<T, Type, KlexTree<T, Type>>(content, value, children) {
    override fun copyWithNewChildren(newChildren: List<KlexTree<T, Type>>): KlexTree<T, Type> = KlexTree(content, value, newChildren)
}