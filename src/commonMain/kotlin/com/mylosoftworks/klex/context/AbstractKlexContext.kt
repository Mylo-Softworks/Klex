package com.mylosoftworks.klex.context

import com.mylosoftworks.klex.Repeat
import com.mylosoftworks.klex.UpTo
import com.mylosoftworks.klex.exceptions.ManualFailError
import com.mylosoftworks.klex.exceptions.NoMatchError
import com.mylosoftworks.klex.exceptions.NotEnoughMatchesError
import com.mylosoftworks.klex.parsing.AbstractKlexTree
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * @param T - The type of the output tree elements
 * @param Source - The source type being parsed (Like string, or an array of tokens)
 * @param Self - A reference to self for properly initializing copies of self.
 */
abstract class AbstractKlexContext<T, Source, Self: AbstractKlexContext<T, Source, Self, ReturnTreeType>, ReturnTreeType:
    AbstractKlexTree<T, Source, ReturnTreeType>>(var remainder: Source, val block: Self.() -> Unit, val create: Self.(remainder: Source, block: Self.() -> Unit) -> Self) {
    /**
     * Result override, used to write errors.
     */
    var error: Throwable? = null
        set(value) {
            if (field == null || value == null) field = value // Field can only be set if it is empty. Or if it is being reset.
        }
    // Sub items, used for constructing the tree at the end of parsing
    val treeSubItems = mutableListOf<ReturnTreeType>()

    var treeValue: T? = null

    @Suppress("unchecked_cast")
    private val self get() = this as Self

    abstract fun parse(): Result<Pair<ReturnTreeType, Source>>

    open fun extraCopy(source: ReturnTreeType) {}

    /**
     * Parses all items in this group context.
     *
     * @param propagateError Whether to consider this fail catastrophic or not, effectively acts like a try-catch.
     */
    fun group(propagateError: Boolean = true, block: Self.() -> Unit): Result<ReturnTreeType> {
        if (error != null) return Result.failure(error!!)

        val (tree, end) = create(self, remainder, block).parse().getOrElse {
            if (propagateError) error = it // Propagates the error from block to this
            return Result.failure(it) }
        remainder = end
        treeSubItems.add(tree)
        extraCopy(tree)
        return Result.success(tree)
    }

    /**
     * Like [group]
     *
     * `group(false, block)`
     */
    fun check(block: Self.() -> Unit): Result<ReturnTreeType> = group(false, block)

    /**
     * Marks this scope as having failed manually. Use `return@name fail()` to also stop execution for this scope.
     * @param message The message to fail with.
     * @return [Unit]
     */
    fun fail(message: String? = null) {
        error = ManualFailError(message)
    }

    /**
     * Discards all blocks which fail to parse until it finds one which does parse.
     */
    fun oneOf(vararg blocks: Self.() -> Unit): Result<ReturnTreeType> {
        if (error != null) return Result.failure(error!!)

        // Items are allowed to fail, but if all fail, this item fails.

        for (block in blocks) {
            val result = create(self, remainder, block).parse()
            if (result.isFailure) continue // If result couldn't be validated
            val (tree, end) = result.getOrThrow() // Already validated that this result is valid, if it's somehow still a failure, something went seriously wrong, so throw
            remainder = end
            treeSubItems.add(tree)
            extraCopy(tree)
            return Result.success(tree)
        }

        error = NoMatchError("Could not find a match.")
        return Result.failure(error!!)
    }

    /**
     * Discards all defined groups which fail to parse until it finds one which does parse.
     */
    fun <U> oneOf(given: U, vararg groups: KlexPlaceholderVal<T, U, Source, Self>): Result<ReturnTreeType> {
        if (error != null) return Result.failure(error!!)

        // Items are allowed to fail, but if all fail, this item fails.

        for (group in groups) {
            val result = group(given, false) // Run the group and override propagateError to false
            if (result.isFailure) continue
            val (tree, _) = result.getOrThrow() // Already validated that this result is valid, if it's somehow still a failure, something went seriously wrong, so throw
            return Result.success(tree)
        }

        error = NoMatchError("Could not find a match.")
        return Result.failure(error!!)
    }

    fun <R> oneOfR(vararg groups: Self.() -> R?): Result<R> {
        if (error != null) return Result.failure(error!!)

        for (group in groups) {
            var returnValue: R? = null
            group(false) {
                val result = group()
                if (error != null && result != null) return@group
                returnValue = result
            }
            if (returnValue != null) return Result.success(returnValue!!)
        }

        error = NoMatchError("Could not find a match.")
        return Result.failure(error!!)
    }

    /**
     * Discards all defined groups which fail to parse until it finds one which does parse.
     */
    fun oneOf(vararg groups: KlexPlaceholderVal<T, Unit, Source, Self>): Result<ReturnTreeType> = oneOf(Unit, *groups)

    data class RepeatResult<ReturnTreeType>(val count: Int, val subTrees: List<ReturnTreeType>)

    /**
     * Repeat n times, based on [Repeat]
     */
    fun repeat(times: Repeat, block: Self.() -> Unit): Result<RepeatResult<ReturnTreeType>> {
        if (error != null) return Result.failure(error!!)

        var hits = 0 // Hit counter
        val subTrees = mutableListOf<ReturnTreeType>()
        var remaining = remainder
        while (times.max == null || hits < times.max) {
            val result = create(self, remaining, block).parse() // Attempt to parse
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
        subTrees.lastOrNull()?.let { extraCopy(it) }

        return Result.success(RepeatResult(hits, subTrees))
    }

    // Placeholder + Define
    class KlexPlaceholderVal<T, V, Source, Self>(val propagateError: Boolean, val value: (Self.(V) -> Unit))
    class KlexPlaceholder<T, V, Source, Self> {
        operator fun getValue(t: Any?, property: KProperty<*>): KlexPlaceholderVal<T, V, Source, Self> {
            return value
        }

        operator fun setValue(t: Any?, property: KProperty<*>, klexPlaceholderVal: KlexPlaceholderVal<T, V, Source, Self>) {
            value = klexPlaceholderVal
        }

        lateinit var value: KlexPlaceholderVal<T, V, Source, Self>
    }

    // First: parameterless version
    // Second: parameter version

    fun placeholder() = KlexPlaceholder<T, Unit, Source, Self>()
    @JvmName("placeholderV")
    fun <V> placeholder() = KlexPlaceholder<T, V, Source, Self>()
    fun <R> placeholderR(): Self.() -> R? = {null}

    fun define(propagateError: Boolean = true, block: (Self.(Unit) -> Unit)) =
        KlexPlaceholderVal<T, Unit, Source, Self>(propagateError, block)
    @JvmName("defineV")
    fun <V> define(propagateError: Boolean = true, block: (Self.(V) -> Unit)) =
        KlexPlaceholderVal<T, V, Source, Self>(propagateError, block)
    fun <R> defineR(block: Self.() -> R) = block

    operator fun KlexPlaceholderVal<T, Unit, Source, Self>.invoke(overridePropagateError: Boolean? = null): Result<Pair<ReturnTreeType, Source>> = this.invoke(Unit, overridePropagateError) // Call regular version
    operator fun <V> KlexPlaceholderVal<T, V, Source, Self>.invoke(given: V, overridePropagateError: Boolean? = null): Result<Pair<ReturnTreeType, Source>> {
        if (this@AbstractKlexContext.error != null) return Result.failure(this@AbstractKlexContext.error!!)

        val usedPGError = overridePropagateError ?: propagateError

        val (tree, end) = create(self, remainder) { value(given) }.parse().getOrElse {
            if (usedPGError) error = it // Propagates the error from block to this
            return Result.failure(it) }

        remainder = end
        treeSubItems.add(tree)
        extraCopy(tree)
        return Result.success(tree to remainder)
    }

    // Operator syntax rules

    // Repeat
    operator fun Repeat.invoke(block: Self.() -> Unit) = repeat(this, block)
    operator fun Int.invoke(block: Self.() -> Unit) = if (this < 0) repeat(UpTo(-this), block) else repeat(Repeat(this), block)
    operator fun IntRange.invoke(block: Self.() -> Unit) = repeat(Repeat(first, last), block)
    operator fun Pair<Int, Int>.invoke(block: Self.() -> Unit) = repeat(Repeat(first, second), block)

    // Result extras
    fun <R> Result<R>.runIfError(block: () -> Unit): Result<R> {
        if (isFailure) block()
        return this
    }
}