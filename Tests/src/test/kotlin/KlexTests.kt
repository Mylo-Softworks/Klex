import com.mylosoftworks.klex.AnyCount
import com.mylosoftworks.klex.Klex
import com.mylosoftworks.klex.Optional
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KlexTests {
    @Test
    fun numberCounter() {
        val parser = Klex.create<String> {
            +"Parse me!"
            AnyCount {
                oneOf({
                    +" One"
                    treeValue = "Number1"
                }, {
                    +" Two"
                    treeValue = "Number2"
                })
            }
            treeValue = "This is a test"
        }

        val result = parser.parse("Parse me! One Two One One One Two Two").getOrThrow().flattenNullValues()[0]
        assertEquals(result.findAll { it.value == "Number1" }.count(), 4)
        assertEquals(result.findAll { it.value == "Number2" }.count(), 3)
    }

    @Test
    fun testRange() {
        val lettersOnly = Klex.create<Unit> {
            -"a-zA-Z"
        }

        assert(lettersOnly.parse("x").isSuccess)
        assert(lettersOnly.parse("C").isSuccess)
        assert(lettersOnly.parse("t").isSuccess)
        assert(lettersOnly.parse("Z").isSuccess)
        assert(lettersOnly.parse("0").isFailure)
        assert(lettersOnly.parse("*").isFailure)
        assert(lettersOnly.parse("#").isFailure)
    }

    @Test
    fun testFallback() {
        val parser = Klex.create<String> {
            AnyCount {
                group(false) {
                    +"Try matching this"
                    treeValue = "Match"
                }.runIfError {
                    // Something went wrong
                    group {
                        +"Alternative"
                        treeValue = "Alt"
                    }
                }
            }
            treeValue = "Root"
        }

        assertNotNull(parser.parse("Try matching this").getOrThrow().find { it.value == "Match" })
        assertNotNull(parser.parse("Alternative").getOrThrow().find { it.value == "Alt" })
    }

    @Test
    fun testConvert() {
        val parser = Klex.create<String> {
            +"Parse me!"
            AnyCount {
                oneOf({
                    +" One"
                    treeValue = "Number1"
                }, {
                    +" Two"
                    treeValue = "Number2"
                })
            }
            treeValue = "This is a test"
        }

        val result = parser.parse("Parse me! One Two One One One Two Two").getOrThrow().flattenNullValues()[0]
        val converted = "[" + result.convert { strContent, value, children -> value + children.joinToString(", ", "[", "]") } + "]"
        assertEquals(converted, "[This is a test[Number1[], Number2[], Number1[], Number1[], Number1[], Number2[], Number2[]]]")
    }

    @Test
    fun testPlaceholder() {
        val klex = Klex.create<Unit> {
            var test by placeholder()
            var test2 by placeholder<String>()

            test = define {
                +"test "

                Optional {
                    test2("test2") // <-- Used before finished defining
                }
            }

            test2 = define<String> {
                +"$it "

                Optional {
                    test()
                }
            }

            test()
        }

        assertDoesNotThrow {
            klex.parse("test test2 test ").getOrThrow()
        }
    }

    @Test
    fun testFindByIndex() {
        val klex = Klex.create<Boolean> {
            +"I want the index of "
            group {
                +"this segment"
                treeValue = true
            }
            +"!"
            treeValue = false
        }
        val result = klex.parse("I want the index of this segment!").getOrThrow().flattenNullValues()[0]
        // KlexTree(start=0, end=33, strContent='I want the index of this segment!', value=false, children=[KlexTree(start=20, end=32, strContent='this segment', value=true, children=[])])
        assert(result.findByIndex(20)?.value ?: false) // Anywhere from 20 until 32 should work
    }

    @Test
    fun testTokenize() {
        abstract class TokenExample(val content: String) // Base class
        class AToken(content: String): TokenExample(content)
        class BToken(content: String): TokenExample(content)
        val klexTokenizer = Klex.create<TokenExample> {
            val whiteSpace = define { -" \n\t" }
            val aToken = define {
                val content = group {
                    +"a"
                    AnyCount {
                        -"a-zA-Z0-9"
                    }
                }.getOrElse { return@define }.content
                treeValue = AToken(content)
            }
            val bToken = define {
                val content = group {
                    +"b"
                    AnyCount {
                        -"a-zA-Z0-9"
                    }
                }.getOrElse { return@define }.content
                treeValue = BToken(content)
            }

            AnyCount {
                Optional { whiteSpace() }
                oneOf(aToken, bToken)
                Optional { whiteSpace() }
            }
        }
        val tokensTree = klexTokenizer.parse("atoken btoken anythingStartingWithAIsAnAToken bAndAnythingStartingWithABIsABToken").getOrThrow().flattenNullValues() // Should be a flat tree now
        val tokensList = tokensTree.map { it.value!! }
        val klexParser = Klex.create<Unit, TokenExample> { // The parser restructures the tokens into a tree
            AnyCount {
                group {
                    val token = match<AToken>().getOrElse { return@group } // Example, accessing AToken object from match.
//                    println(token.content) // Example: Printing the token's content
                    treeValue = Unit
                }
                group {
                    match<BToken>() // Here, we ignore the token
                    treeValue = Unit
                }
                treeValue = Unit // Prevent deletion on flatten
            }
        }
        val parseResult = klexParser.parse(tokensList).getOrThrow().flattenNullValues()
        assertEquals(2, parseResult.size) // 2 pairs of a, b. [[a, b], [a, b]]
    }

    @Test
    fun testListFuns() {
        val klexParser = Klex.create<Unit, String> { // Unit return type, String tokens (not to be confused with the raw lexer)
            val token1 = funCond { this == "1" } // "this" is the next captured token, in this case a string
            val other = funType<String>() // By selecting string, it'll always capture in this context (list<String>)
            AnyCount {
                oneOf({
                    token1()

                    treeValue = Unit // Make non-null, so this tree item won't be removed for the assert.
                }, {
                    other() // Fallback, capture anyways
                })
                println(remainder)
            }
            treeValue = Unit // To ensure the root is set, so flattenNullValues will give an array that's always 1 in size.
        }
        val parseResult = klexParser.parse("1:2:1:4:1".split(":")).getOrThrow().flattenNullValues()[0]
        assertEquals(3, parseResult.findAll { it.content[0] == "1" }.size) // content[0] is used to get the raw input value, in this example trees don't contain other trees
    }
}