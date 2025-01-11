# Klex
A flexible lexer written in kotlin. Everything you'll need to go from a source file to an [abstract syntax tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree).

Practical examples (json parser, programming language parser) coming soon.

# Usage
Usage of Klex is practically 2 steps, writing definitions and running them.

You can use write definitions by instantiating a Klex object, and writing your definitions inside.
```kotlin
import com.mylosoftworks.klex.Klex

fun main() {
    val parser = Klex<Unit> { // Unit used here, but this can be used for storing information in the tree items for parsing
        // Definitions go here.
    }
}
```

## KlexContext
The KlexContext object is used with your rules, unlike in [GBNF-Kotlin](https://github.com/Mylo-Softworks/GBNF-Kotlin), code in Klex is executed as it is parsed. Like immediate mode, but for lexing and parsing.

Useful fields to know about:
* `treeValue` - A value of type T? (given when creating Klex<T> object) used to store value for the current tree item.
* `error` (Handled automatically) - Stores an error if something went wrong, propagated up from groups unless the group has propagateError set to false.
* `treeSubItems` (Handled automatically) - The children for the generated tree.

## Rules
Rules are used for matching text, there are group rules and content rules. Some rules have an alternative simpler syntax.

### Groups
Group:
```kotlin
group {
    // Put content in here

    treeValue = "Value" // Assuming we're in a Klex<String> context.
}

// propagateError set to false, when an error occurs inside of this group, don't consider the parent context to have failed.
group(false) { // or check {}
    
}.runIfError { // Optional, run some code if something went wrong in the group
    // Note that we do not have access to the failed group, instead, we can create a new group.
    // runIfError effectively acts as "else" for failed matches, not to be confused with oneOf groups, which are not meant for error checking
}
```
One of:
```kotlin
oneOf({
    // Option 1
}, {
    // Option 2
}) // As many options as you need, given as vararg blocks: KlexContext<T>.() -> Unit
```
Repeat:  
Available rules:
* `Repeat(min: Int, val max: Int? = min)`
* `UpTo(max: Int?)` // Set null to effectively get the same as AnyCount
* `AtLeast(min: Int)`
* `Optional`
* `OneOrMore`
* `AnyCount`

```kotlin
val matchCount = repeat(UpTo(5)) { // Repeats up to 5 times
    
}.getOrElse { return@Klex }.count // count, subTrees
// Alternatively, just invoke the repeat type
val matchCount = UpTo(5) {
    
}.getOrElse { return@Klex }.count
```

### Content
Literal:
```kotlin
literal("content")
// Alternatively, use +"content"
+"content"
```
Range:
```kotlin
range("a-zA-Z") // Regex style ranges
range("a-zA-Z", true) // Can be negated
// Alternatively, use -"range" and -!"range"
-"a-zA-Z" // Regex style ranges
-!"a-zA-Z" // Can be negated
```

## KlexTree
After parsing, you will receive a KlexTree object, which is a tree with it's matched string content, its assigned value (from in the KlexContext) and the child tree items.  

```kotlin
val flatTree = tree.flattenNullValues() // Every item with no value (treeValue) set will be removed, and it's children will be merged upwards, this will greatly reduce the total amount of tree items, and leave only the items you need.
val found = tree.find { it.value == "test" } // Find the first item which matches the predicate, or null if no matches were found
val allFound = tree.findAll { it.value == "test" } // Find all children which match the predicate
val converted = "[" + result.convert { strContent, value, children -> value + children.joinToString(", ", "[", "]") } + "]" // Convert the tree to a custom structure
// converted will output a string (used as tree structure here) like (assuming all values are just "value") [value[value[], value[value[]]]]
```