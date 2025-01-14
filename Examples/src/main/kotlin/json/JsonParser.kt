package json

import com.mylosoftworks.klex.AnyCount
import com.mylosoftworks.klex.Klex
import com.mylosoftworks.klex.Optional
import com.mylosoftworks.klex.parsing.KlexTree

object JsonParser {
    private val klex = Klex<JsonElement<*>> {
        var element by placeholder()

        val ws = define {
            AnyCount {
                -" \t\n" // Accept space, tab and newline as whitespace characters
            }
        }

        // Primitive types
        val string = define { // Parses a json string
            +"\"" // Starts with a quote
            val strcontents = group {
                AnyCount {
                    oneOf({
                        +"\\\\" // Escaped backslash (\\) should be captured
                    }, {
                        +"\\\"" // Escaped quotes (\") should be captured
                    }, {
                        -!"\"" // Anything except for quotes can now be captured, escapes are handled on the previous options
                    })
                }
            }.getOrElse { return@define }.strContent // ensures we don't continue if string failed to parse
            treeValue = JsonString(strcontents)

            +"\"" // Ends with a quote
        }
        val number = define {
            val numString = group {
                AnyCount {
                    -"0-9"
                }
                Optional {
                    +"."
                }
                AnyCount {
                    -"0-9"
                }
                Optional {
                    -"eE" // Case-insensitive
                    AnyCount {
                        -"0-9"
                    }
                }
            }.getOrElse { return@define }.strContent

            val parsed = numString.toDoubleOrNull()
            if (parsed == null) fail() // Fail makes the parent not accept this answer, I use it here because I am lazy and don't feel like making the number parser more accurate
            else treeValue = JsonNumber(parsed)
        }
        val bool = define {
            val boolString = oneOf({+"true"}, {+"false"}).getOrElse { return@define }.strContent
            treeValue = JsonBoolean(boolString == "true")
        }
        val nullJson = define {
            +"null"
            treeValue = JsonNull()
        }

        // Group values
        val array = define {
            +"["
            val children = group {
                Optional {
                    element()
                    AnyCount {
                        +","
                        element()
                    }
                }
            }.getOrElse { return@define }.parseArrayContentShort()
            +"]"
            treeValue = children
        }
        val objEntry = define {
            // key: value
            ws()
            val key = (string().getOrElse { return@define }.first.value as JsonString).value
            ws()
            +":"
            ws()
            val value = element().getOrElse { return@define }.first
            ws()

            treeValue = JsonObjectEntry(key to value.parseShort())
        }
        val obj = define {
            +"{"
            val keyValues = group {
                Optional {
                    objEntry()
                    AnyCount {
                        +","
                        objEntry()
                    }
                }
            }.getOrElse { return@define }.parseObjKeyValShort()
            +"}"
            treeValue = keyValues
        }

        // Define element
        element = define {
            ws()
            oneOf(string, number, bool, nullJson, array, obj)
            ws()
        }

        element()
    }

    fun parse(string: String): Result<JsonElement<*>> {
        return Result.success(klex.parse(string).getOrElse { return Result.failure(it) }.parseShort())
    }

    private fun KlexTree<JsonElement<*>>.parseShort(): JsonElement<*> {
        return this.flattenNullValues()[0].convert {_, value: JsonElement<*>?, _ -> value!!}
    }

    private fun KlexTree<JsonElement<*>>.parseArrayContentShort(): JsonArray {
        return JsonArray(this.flattenNullValues().map { it.convert {_, value: JsonElement<*>?, _ -> value!!} }.toMutableList())
    }

    private fun KlexTree<JsonElement<*>>.parseObjKeyValShort(): JsonObject {
        val parsed = (this.flattenNullValues().map { it.convert {_, value: JsonElement<*>?, _ -> value!!} } as List<JsonObjectEntry>).map { it.value }.toTypedArray() // All direct values will be JsonObjectEntry
        return JsonObject(hashMapOf(*parsed))
    }
}

fun main() {
    println(JsonParser.parse("""
        [
            {"key": "value", "key2": 100, "key3": true},
            {"key": "value2", "key2": 200, "key3": false}
        ]
    """.trimIndent()).getOrThrow())
}