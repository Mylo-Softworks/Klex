package com.mylosoftworks.klex

import com.mylosoftworks.klex.context.AbstractKlexContext
import com.mylosoftworks.klex.context.KlexContextList
import com.mylosoftworks.klex.context.KlexContextString
import com.mylosoftworks.klex.exceptions.*
import com.mylosoftworks.klex.parsing.KlexStringTree
import com.mylosoftworks.klex.parsing.KlexTree
import kotlin.js.JsName
import kotlin.jvm.JvmName

/**
 * The main utility for creating a Klex parser.
 *
 * @see create For creating a context.
 */
class Klex<T, Source, KlexContext: AbstractKlexContext<T, Source, KlexContext, ReturnTreeType>, ReturnTreeType: KlexTree<T, Source>> private constructor(val block: KlexContext.() -> Unit, val createContext: (remainder: Source, block: KlexContext.() -> Unit) -> KlexContext) {
    fun parse(source: Source): Result<ReturnTreeType> {
        val (tree, end) = createContext(source, block).parse().getOrElse { return Result.failure(it) }

        if (end is String && end.isNotEmpty()) return Result.failure(NotDoneParsingError("Not done parsing, remaining: $end"))
        if (end is Array<*> && end.isNotEmpty()) return Result.failure(NotDoneParsingError("Not done parsing, remaining: $end"))
        if (end is Collection<*> && end.isNotEmpty()) return Result.failure(NotDoneParsingError("Not done parsing, remaining: $end"))

        return Result.success(tree)
    }

    companion object {
        /**
         * Create a Klex context for parsing strings.
         * @param T The type of tree item to output to.
         */
        fun <T> create(block: KlexContextString<T>.() -> Unit): Klex<T, String, KlexContextString<T>, KlexStringTree<T>> = Klex(block, {remainder, block -> KlexContextString(remainder, block) })

        /**
         * Create a Klex context for parsing tokens of type [Source].
         * @param T The type of tree item to output to.
         * @param Source The source token type to parse.
         */
        @JvmName("createSource")
        @JsName("createSource")
        fun <T, Source : Any> create(block: KlexContextList<T, Source>.() -> Unit): Klex<T, List<Source>, KlexContextList<T, Source>, KlexTree<T, List<Source>>> = Klex(block, {remainder, block -> KlexContextList(remainder, block) })
    }
}
