package com.mylosoftworks.klex

open class Repeat(val min: Int, val max: Int? = min)
class UpTo(max: Int?): Repeat(0, max)
class AtLeast(min: Int): Repeat(min, null)
object Optional: Repeat(0, 1)
object OneOrMore: Repeat(1, null)
object AnyCount: Repeat(0, null)