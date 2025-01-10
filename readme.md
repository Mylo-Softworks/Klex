# Klex
A flexible lexer written in kotlin. Everything you'll need to go from a source file to an [abstract syntax tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree).

TODO: Write documentation

# Usage
Usage of Klex is practically 2 steps, writing definitions and running them.

You can use write definitions using the DSL by instantiating a Klex object, and writing your definitions inside.
```kotlin
import com.mylosoftworks.klex.Klex

fun main() {
    val parser = Klex {
        // Definitions go here.
    }
}
```