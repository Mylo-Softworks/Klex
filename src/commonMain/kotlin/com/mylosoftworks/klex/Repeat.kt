package com.mylosoftworks.klex

import com.mylosoftworks.klex.Optional.max
import com.mylosoftworks.klex.Optional.min

/**
 * Indicates a repeat from min to max times (inclusive)
 *
 * Variants:
 * * [Repeat]
 * * [UpTo]
 * * [AtLeast]
 * * [Optional]
 * * [OneOrMore]
 * * [AnyCount]
 */
open class Repeat(val min: Int, val max: Int? = min)

/**
 * Repeat up to [max] times, or unlimited if [max] is `null`.
 *
 * Equivalent to [Repeat] with [min] = `0`, [max] = [max]
 */
class UpTo(max: Int?): Repeat(0, max)

/**
 * Repeat at least [min] times.
 *
 * Equivalent to [Repeat] with [min] = [min], [max] = `null`
 */
class AtLeast(min: Int): Repeat(min, null)

/**
 * Try to capture, but fine if it fails.
 *
 * Equivalent to [Repeat] with [min] = `0`, [max] = `1`
 */
object Optional: Repeat(0, 1)

/**
 * Require at least one successful capture, and try to capture as many as possible.
 *
 * Equivalent to [Repeat] with [min] = `1`, [max] = `null`
 */
object OneOrMore: Repeat(1, null)

/**
 * Try to capture as many as possible.
 *
 * Equivalent to [Repeat] with [min] = `0`, [max] = `null`
 */
object AnyCount: Repeat(0, null)