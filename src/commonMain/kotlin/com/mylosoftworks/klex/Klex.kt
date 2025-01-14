package com.mylosoftworks.klex

import com.mylosoftworks.klex.exceptions.*
import com.mylosoftworks.klex.parsing.KlexTree
import com.mylosoftworks.klex.parsing.RangeParser
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * The main utility for creating a Klex parser.
 *
 * @param block A code block containing code to execute to parse the contents, using [KlexContext].
 */
class Klex<T>(val block: KlexContext<T>.() -> Unit) {
    fun parse(string: String): Result<KlexTree<T>> {
        val (tree, end) = KlexContext(string, block).parse().getOrElse { return Result.failure(it) }
        if (end.isNotEmpty()) return Result.failure(NotDoneParsingError("Not done parsing, remaining: $end"))
        return Result.success(tree)
    }
}

/**
 * Class used for context in Klex.
 *
 * @param remainder The remaining text left to parse.
 */
class KlexContext<T>(var remainder: String, val block: KlexContext<T>.() -> Unit) {
    /**
     * Result override, used to write errors.
     */
    var error: Throwable? = null
        set(value) {
            if (field == null || value == null) field = value // Field can only be set if it is empty. Or if it is being reset.
        }
    // Sub items, used for constructing the tree at the end of parsing
    val treeSubItems = mutableListOf<KlexTree<T>>()

    var treeValue: T? = null

    fun parse(): Result<Pair<KlexTree<T>, String>> {
        block()
        if (error != null) return Result.failure(error!!)
        return Result.success(
            KlexTree(
            treeSubItems.joinToString("") { it.strContent },
            treeValue,
            treeSubItems
        ) to remainder)
    }

    // Group rules

    /**
     * Parses all items in this group context.
     *
     * @param propagateError Whether to consider this fail catastrophic or not, effectively acts like a try-catch.
     * @param mergeUp Merge the [KlexTree] up, in other words, remove the items and
     */
    fun group(propagateError: Boolean = true, block: KlexContext<T>.() -> Unit): Result<KlexTree<T>> {
        if (error != null) return Result.failure(error!!)

        val (tree, end) = KlexContext(remainder, block).parse().getOrElse {
            if (propagateError) error = it // Propagates the error from block to this
            return Result.failure(it) }
        remainder = end
        treeSubItems.add(tree)
        return Result.success(tree)
    }

    /**
     * Like [group]
     *
     * `group(false, block)`
     */
    fun check(block: KlexContext<T>.() -> Unit): Result<KlexTree<T>> = group(false, block)

    /**
     * Marks this scope as having failed manually
     */
    fun fail(message: String? = null) {
        error = ManualFailError(message)
    }

    /**
     * Discards all blocks which fail to parse until it finds one which does parse.
     */
    fun oneOf(vararg blocks: KlexContext<T>.() -> Unit): Result<KlexTree<T>> {
        if (error != null) return Result.failure(error!!)

        // Items are allowed to fail, but if all fail, this item fails.

        for (block in blocks) {
            val result = KlexContext(remainder, block).parse()
            if (result.isFailure) continue // If result couldn't be validated
            val (tree, end) = result.getOrThrow() // Already validated that this result is valid, if it's somehow still a failure, something went seriously wrong, so throw
            remainder = end
            treeSubItems.add(tree)
            return Result.success(tree)
        }

        error = NoMatchError("Could not find a match.")
        return Result.failure(error!!)
    }

    /**
     * Discards all defined groups which fail to parse until it finds one which does parse.
     */
    fun <U> oneOf(given: U, vararg groups: KlexPlaceholderVal<T, U>): Result<KlexTree<T>> {
        if (error != null) return Result.failure(error!!)

        // Items are allowed to fail, but if all fail, this item fails.

        for (group in groups) {
            val result = group(given, false) // Run the group and override propagateError to false
            if (result.isFailure) continue
            val (tree, end) = result.getOrThrow() // Already validated that this result is valid, if it's somehow still a failure, something went seriously wrong, so throw
            return Result.success(tree)
        }

        error = NoMatchError("Could not find a match.")
        return Result.failure(error!!)
    }

    /**
     * Discards all defined groups which fail to parse until it finds one which does parse.
     */
    fun oneOf(vararg groups: KlexPlaceholderVal<T, Unit>): Result<KlexTree<T>> = oneOf(Unit, *groups)

    data class RepeatResult<T>(val count: Int, val subTrees: List<KlexTree<T>>)

    /**
     * Repeat n times, based on [Repeat]
     */
    fun repeat(times: Repeat, block: KlexContext<T>.() -> Unit): Result<RepeatResult<T>> {
        if (error != null) return Result.failure(error!!)

        var hits = 0 // Hit counter
        val subTrees = mutableListOf<KlexTree<T>>()
        var remaining = remainder
        while (times.max == null || hits < times.max) {
            val result = KlexContext(remaining, block).parse() // Attempt to parse
            if (result.isFailure) break // Failed
            val (tree, end) = result.getOrThrow()

            remaining = end
            subTrees.add(tree)
            hits++
        }

        if (hits < times.min) {
            error = NotEnoughMatchesError("Not enough matches found, need ${times.min} but got $hits")
            return Result.failure(error!!)
        }

        remainder = remaining
        treeSubItems.addAll(subTrees)

        return Result.success(RepeatResult(hits, subTrees))
    }

    // Content rules
    fun literal(literal: String): Result<KlexTree<T>> {
        if (error != null) return Result.failure(error!!)

        if (!remainder.startsWith(literal)) {
            error = NoMatchError("String doesn't match literal \"$literal\"")
            return Result.failure(error!!)
        }

        val treeItem = KlexTree<T>(literal, null, listOf())
        remainder = remainder.substring(literal.length)
        treeSubItems.add(treeItem)
        return Result.success(treeItem)
    }

    fun range(range: String, negate: Boolean = false): Result<KlexTree<T>> {
        if (error != null) return Result.failure(error!!)
        if (remainder.isEmpty()) {
            error = NoTextLeftError("No text left to parse with this range.")
            return Result.failure(error!!)
        }

        val char = remainder[0]

        if (RangeParser.matchesRange(char, range) == negate) { // True if failed
            error = NoMatchError("Char doesn't match range [$range]")
            return Result.failure(error!!)
        }

        val treeItem = KlexTree<T>(char.toString(), null, listOf())
        remainder = remainder.substring(1)
        treeSubItems.add(treeItem)
        return Result.success(treeItem)
    }

    // Operator syntax rules

    // Repeat
    operator fun Repeat.invoke(block: KlexContext<T>.() -> Unit) = repeat(this, block)
    operator fun Int.invoke(block: KlexContext<T>.() -> Unit) = if (this < 0) repeat(UpTo(-this), block) else repeat(Repeat(this), block)
    operator fun IntRange.invoke(block: KlexContext<T>.() -> Unit) = repeat(Repeat(first, last), block)
    operator fun Pair<Int, Int>.invoke(block: KlexContext<T>.() -> Unit) = repeat(Repeat(first, second), block)

    // Content rules
    operator fun String.unaryPlus() = literal(this)
    operator fun String.unaryMinus() = range(this)
    operator fun String.not() = this to true
    operator fun Pair<String, Boolean>.not() = this.first to !this.second
    operator fun Pair<String, Boolean>.unaryMinus() = range(this.first, this.second)

    // Result extras
    fun <R> Result<R>.runIfError(block: () -> Unit): Result<R> {
        if (isFailure) block()
        return this
    }

    // Placeholder + Define
    class KlexPlaceholderVal<T, V>(val propagateError: Boolean, val value: (KlexContext<T>.(V) -> Unit))
    class KlexPlaceholder<T, V> {
        operator fun getValue(t: Any?, property: KProperty<*>): KlexPlaceholderVal<T, V> {
            return value
        }

        operator fun setValue(t: Any?, property: KProperty<*>, klexPlaceholderVal: KlexPlaceholderVal<T, V>) {
            value = klexPlaceholderVal
        }

        lateinit var value: KlexPlaceholderVal<T, V>
    }

    // First: parameterless version
    // Second: parameter version

    fun placeholder() = KlexPlaceholder<T, Unit>()
    @JvmName("placeholderV")
    fun <V> placeholder() = KlexPlaceholder<T, V>()

    fun define(propagateError: Boolean = true, block: (KlexContext<T>.(Unit) -> Unit)) = KlexPlaceholderVal(propagateError, block)
    @JvmName("defineV")
    fun <V> define(propagateError: Boolean = true, block: (KlexContext<T>.(V) -> Unit)) = KlexPlaceholderVal(propagateError, block)

    operator fun KlexPlaceholderVal<T, Unit>.invoke(overridePropagateError: Boolean? = null): Result<Pair<KlexTree<T>, String>> = this.invoke(Unit, overridePropagateError) // Call regular version
    operator fun <V> KlexPlaceholderVal<T, V>.invoke(given: V, overridePropagateError: Boolean? = null): Result<Pair<KlexTree<T>, String>> {
        if (this@KlexContext.error != null) return Result.failure(this@KlexContext.error!!)

        val usedPGError = overridePropagateError ?: propagateError

        val (tree, end) = KlexContext(remainder) { value(given) }.parse().getOrElse {
            if (usedPGError) error = it // Propagates the error from block to this
            return Result.failure(it) }

        remainder = end
        treeSubItems.add(tree)
        return Result.success(tree to remainder)
    }
}