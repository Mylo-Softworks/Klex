package com.mylosoftworks.klex

/**
 *
 *
 * @param T Data type for the output tree.
 */
class Klex<T>() {

    fun createDefinitions(block: Klex<T>.() -> Unit) = apply(block)
}