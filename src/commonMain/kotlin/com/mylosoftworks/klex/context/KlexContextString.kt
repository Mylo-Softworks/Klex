package com.mylosoftworks.klex.context

import com.mylosoftworks.klex.exceptions.NoMatchError
import com.mylosoftworks.klex.exceptions.NoTokensLeftError
import com.mylosoftworks.klex.parsing.KlexStringTree
import com.mylosoftworks.klex.parsing.KlexTree
import com.mylosoftworks.klex.parsing.RangeParser

class KlexContextString<T>(remainder: String, block: KlexContextString<T>.() -> Unit):
    AbstractKlexContext<T, String, KlexContextString<T>, KlexStringTree<T>>(remainder, block, {remainder, block -> KlexContextString(remainder, block) }) {

    // Content rules
    fun literal(literal: String): Result<KlexTree<T, String>> {
        if (error != null) return Result.failure(error!!)

        if (!remainder.startsWith(literal)) {
            error = NoMatchError("String doesn't match literal \"$literal\"")
            return Result.failure(error!!)
        }

        val treeItem = KlexStringTree<T>(literal, null, listOf())
        remainder = remainder.substring(literal.length)
        treeSubItems.add(treeItem)
        return Result.success(treeItem)
    }

    fun range(range: String, negate: Boolean = false): Result<KlexTree<T, String>> {
        if (error != null) return Result.failure(error!!)
        if (remainder.isEmpty()) {
            error = NoTokensLeftError("No text left to parse with this range.")
            return Result.failure(error!!)
        }

        val char = remainder[0]

        if (RangeParser.matchesRange(char, range) == negate) { // True if failed
            error = NoMatchError("Char doesn't match range [$range]")
            return Result.failure(error!!)
        }

        val treeItem = KlexStringTree<T>(char.toString(), null, listOf())
        remainder = remainder.substring(1)
        treeSubItems.add(treeItem)
        return Result.success(treeItem)
    }

    // Operator syntax rules

    // Content rules
    operator fun String.unaryPlus() = literal(this)
    operator fun String.unaryMinus() = range(this)
    operator fun String.not() = this to true
    operator fun Pair<String, Boolean>.not() = this.first to !this.second
    operator fun Pair<String, Boolean>.unaryMinus() = range(this.first, this.second)
    override fun parse(): Result<Pair<KlexStringTree<T>, String>> {
        block(this)
        if (error != null) return Result.failure(error!!)
        return Result.success(
            KlexStringTree(
                treeSubItems.joinToString("") { it.content },
                treeValue,
                treeSubItems
            ) to remainder)
    }
}