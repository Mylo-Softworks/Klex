package com.mylosoftworks.klex.exceptions

/**
 * Could not find a valid match.
 */
class NoMatchError(message: String? = null): ParseError(message)