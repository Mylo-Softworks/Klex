package com.mylosoftworks.klex.context

import com.mylosoftworks.klex.exceptions.NoMatchError
import com.mylosoftworks.klex.exceptions.NoTokensLeftError
import com.mylosoftworks.klex.parsing.KlexTree
import kotlin.reflect.KClass

class KlexContextList<T, Source: Any>(remainder: List<Source>, block: KlexContextList<T, Source>.() -> Unit):
    AbstractKlexContext<T, List<Source>, KlexContextList<T, Source>, KlexTree<T, List<Source>>>(remainder, block, { newRemainder, newBlock -> KlexContextList(newRemainder, newBlock) }) {

    fun takeNextToken(): Result<Source> {
        if (error != null) return Result.failure(error!!)
        if (remainder.isEmpty()) {
            error = NoTokensLeftError("No tokens left to parse.")
            return Result.failure(error!!)
        }
        return Result.success(remainder[0])
    }
    /**
     * Where current token matches predicate, match
     */
    fun match(predicate: Source.() -> Boolean): Result<Source> {
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

        return Result.success(token)
    }

    /**
     * Where current token is class, match
     */
    @Suppress("unchecked_cast")
    fun <T: Source> match(clazz: KClass<T>) = match { clazz.isInstance(this) } as Result<T>

    /**
     * Where current token is class, match (Reified generic version)
     */
    @Suppress("unchecked_cast")
    inline fun <reified T: Source> match() = match { this is T } as Result<T>

    inline fun <reified T2: Source> funType(): KlexContextList<T, Source>.() -> Result<T2> = { match<T2>() }
    fun funCond(predicate: Source.() -> Boolean): KlexContextList<T, Source>.() -> Result<Source> = { match(predicate) }

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