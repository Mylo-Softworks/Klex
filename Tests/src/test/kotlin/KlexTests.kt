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
        val parser = Klex<String> {
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
        val lettersOnly = Klex<Unit> {
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
        val parser = Klex<String> {
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
        val parser = Klex<String> {
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
        val klex = Klex<Unit> {
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
}