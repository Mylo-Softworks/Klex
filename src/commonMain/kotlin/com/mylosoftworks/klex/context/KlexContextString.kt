package com.mylosoftworks.klex.context

import com.mylosoftworks.klex.exceptions.NoMatchError
import com.mylosoftworks.klex.exceptions.NoTokensLeftError
import com.mylosoftworks.klex.parsing.KlexStringTree
import com.mylosoftworks.klex.parsing.RangeParser

class KlexContextString<T>(remainder: String, block: KlexContextString<T>.() -> Unit, val startIndex: Int):
    AbstractKlexContext<T, String, KlexContextString<T>, KlexStringTree<T>>(remainder, block, {remainder, block -> KlexContextString(remainder, block, currentIndex) }) {

    var currentIndex = startIndex

    override fun extraCopy(source: KlexStringTree<T>) {
        currentIndex = source.sourceEndIndex
    }

    // Content rules
    fun literal(literal: String): Result<KlexStringTree<T>> {
        if (error != null) return Result.failure(error!!)

        if (!remainder.startsWith(literal)) {
            error = NoMatchError("String doesn't match literal \"$literal\"")
            return Result.failure(error!!)
        }

        val length = literal.length
        val treeItem = KlexStringTree<T>(literal, null, listOf(), currentIndex, currentIndex + length)
        remainder = remainder.substring(length)
        treeSubItems.add(treeItem)
        currentIndex += length // Increment the current index by the amount of characters copied
        return Result.success(treeItem)
    }

    fun range(range: String, negate: Boolean = false): Result<KlexStringTree<T>> {
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

        val treeItem = KlexStringTree<T>(char.toString(), null, listOf(), currentIndex, currentIndex + 1)
        remainder = remainder.substring(1)
        treeSubItems.add(treeItem)
        currentIndex += 1 // Increment the current index by 1
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
                treeSubItems,
                startIndex,
                currentIndex
            ) to remainder)
    }
}