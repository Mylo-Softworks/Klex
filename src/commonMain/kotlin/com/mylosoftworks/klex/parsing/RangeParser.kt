package com.mylosoftworks.klex.parsing

/**
 * Object used to parse ranges.
 */
object RangeParser {
    private val cache: HashMap<String, Array<CharRange>> = hashMapOf()

    /**
     * Check if a character matches a range.
     */
    fun matchesRange(char: Char, rangeDef: String): Boolean {
        val ranges = cache.getOrPut(rangeDef) { createRange(rangeDef) }
        return ranges.any {
            it.contains(char)
        }
    }

    private fun createRange(rangeDef: String): Array<CharRange> {
        // Example: [a-fABCDEF0-9\-] should allow all of [abcdefABCDEF0123456789-]
        val currentList = mutableListOf<CharRange>()
        var currentChars = ""
        for (char in rangeDef) {
            if (char == '-') {
                if (currentChars.isEmpty()) error("Range contains \"-\" but doesn't have start char and is not escaped.")
                if (currentChars == "\\") {
                    currentList.add(CharRange('-', '-')) // Escaped hyphen
                    currentChars = ""
                    continue
                }

                currentChars += char
                continue
            }

            if (currentChars.endsWith("-")) { // Next character is end of range
                currentList.add(CharRange(currentChars[0], char))
                currentChars = ""
                continue
            }

            if (currentChars.length == 1) { // Already has a character
                val currentChar = currentChars[0]
                currentList.add(CharRange(currentChar, currentChar))
                currentChars = ""
            }

            currentChars += char
        }

        if (currentChars.isNotEmpty()) {
            val char = currentChars[0]
            currentList.add(CharRange(char, char))
        }

        return currentList.toTypedArray()
    }
}