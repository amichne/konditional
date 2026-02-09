package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContextualRuleValueTest {
    private abstract class CustomContext protected constructor(open val x: Int) : Context {
        companion object {
            operator fun invoke(x: Int): CustomContext = object : CustomContext(x) {}
        }
    }

    private data class SecondaryContext(override val x: Int, val y: String) : CustomContext(x)

    @Test
    fun `contextual rule yields from context`() {
        val namespace = object : Namespace.TestNamespaceFacade("contextual-yield") {
            val computed by integer<CustomContext>(default = 0) {
                rule {
                    extension { x >= 0 }
                } yields {
                    x
                }
            }
        }

        assertEquals(5, namespace.computed.evaluate(CustomContext(5)))
        assertEquals(0, namespace.computed.evaluate(CustomContext(-1)))
    }

    @Test
    fun `covariant context works in rules and yields`() {
        val namespace = object : Namespace.TestNamespaceFacade("covariant-context") {
            val computed by integer<CustomContext>(default = 0) {
                rule {
                    extension { x >= 0 }
                } yields {
                    if (SecondaryContext::class.isInstance(this) && (this as SecondaryContext).y == "yes") {
                        x + 3
                    } else {
                        x
                    }
                }
            }
        }

        assertEquals(10 + 3, namespace.computed.evaluate(SecondaryContext(10, "yes")))
        assertEquals(10, namespace.computed.evaluate(SecondaryContext(10, "no")))
        assertEquals(0, namespace.computed.evaluate(CustomContext(-1)))
    }
}
