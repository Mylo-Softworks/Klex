package com.mylosoftworks.klex.context

import com.mylosoftworks.klex.exceptions.NoMatchError
import com.mylosoftworks.klex.exceptions.NoTokensLeftError
import com.mylosoftworks.klex.parsing.KlexTree
import kotlin.reflect.KClass

class KlexContextList<T, Source: Any>(remainder: List<Source>, block: KlexContextList<T, Source>.() -> Unit):
    AbstractKlexContext<T, List<Source>, KlexContextList<T, Source>, KlexTree<T, List<Source>>>(remainder, block, {remainder, block -> KlexContextList(remainder, block) }) {

    fun takeNextToken(): Result<Source> {
        if (error != null) return Result.failure(error!!)
        if (remainder.isEmpty()) {
            error = NoTokensLeftError("No tokens left to parse.")
            return Result.failure(error!!)
        }
        return Result.success(remainder[0])
    }
    /**
     * Where current token matches predicate
     */
    fun where(predicate: Source.() -> Boolean): Result<KlexTree<T, List<Source>>> {
        if (error != null) return Result.failure(error!!)
        val token = takeNextToken().getOrElse { return Result.failure(it) }
        val match = predicate(token)
        if (!match) {
            error = NoMatchError("Token doesn't match predicate")
            return Result.failure(error!!)
        }

        val treeItem = KlexTree<T, List<Source>>(listOf(token), null, listOf())
        remainder = remainder.drop(1)
        treeSubItems.add(treeItem)

        return Result.success(treeItem)
    }

    /**
     * Where current token is class
     */
    fun <T: Source> where(clazz: KClass<T>) = where { clazz.isInstance(this) }

    /**
     * Where current token is class (Reified generic version)
     */
    inline fun <reified T: Source> where() = where { this is T }

    override fun parse(): Result<Pair<KlexTree<T, List<Source>>, List<Source>>> {
        block(this)
        if (error != null) return Result.failure(error!!)
        return Result.success(
            KlexTree(
                treeSubItems.flatMap { it.content },
                treeValue,
                treeSubItems
            ) to remainder)
    }
}