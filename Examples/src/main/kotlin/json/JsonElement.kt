package json

/**
 * Base class for json elements, nothing special because this is just an example.
 */
abstract class JsonElement<T: Any>(val value: T) {
    override fun toString(): String { // Helps printing
        return "${this.javaClass.simpleName}($value)"
    }
}

// Elements with element contents
class JsonObject(value: HashMap<String, JsonElement<*>>): JsonElement<HashMap<String, JsonElement<*>>>(value)
class JsonObjectEntry(value: Pair<String, JsonElement<*>>): JsonElement<Pair<String, JsonElement<*>>>(value) // Only used for parsing
class JsonArray(value: MutableList<JsonElement<*>>): JsonElement<MutableList<JsonElement<*>>>(value)

// Elements with values
class JsonString(value: String): JsonElement<String>(value)
class JsonNumber(value: Double): JsonElement<Double>(value) // Let's use double
class JsonBoolean(value: Boolean): JsonElement<Boolean>(value)

// Elements without values
class JsonNull: JsonElement<Unit>(Unit)