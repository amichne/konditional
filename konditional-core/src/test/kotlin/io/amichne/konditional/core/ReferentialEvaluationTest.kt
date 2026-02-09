package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.evaluation.EvaluationScope
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ReferentialEvaluationTest {
    @Test
    fun `referential evaluation is lazy and cached`() {
        val evaluations = AtomicInteger(0)

        data class CustomContext(val x: Int) : Context

        data class DataOut(
            val y: Int = 0,
        ) : Konstrained<ObjectSchema> {
            override val schema = schemaRoot {
                ::y of { minimum = 0 }
            }
        }

        data class DataOutRef(
            val z: Int = 0,
        ) : Konstrained<ObjectSchema> {
            override val schema = schemaRoot {
                ::z of { minimum = 0 }
            }
        }

        val exampleValue: CustomContext.() -> DataOut = {
            DataOut(y = x).also { evaluations.incrementAndGet() }
        }

        val flags =
            object : Namespace.TestNamespaceFacade("referential-evaluation") {
                private val referentialValue: EvaluationScope<CustomContext, Namespace>.() -> DataOutRef = {
                    DataOutRef(z = example().y + example().y)
                }

                val example by custom<DataOut, CustomContext>(DataOut()) {
                    rule {
                        extension { x >= 0 }
                    } yields exampleValue
                }

                val referentialExample by custom<DataOutRef, CustomContext>(DataOutRef()) {
                    rule {
                        extension { x >= 0 }
                    } yields referentialValue
                }
            }

        val negativeContext = CustomContext(x = -1)
        val positiveContext = CustomContext(x = 5)

        assertEquals(DataOutRef(), flags.referentialExample.evaluate(negativeContext))
        assertEquals(0, evaluations.get())

        assertEquals(DataOutRef(z = 10), flags.referentialExample.evaluate(positiveContext))
        assertEquals(1, evaluations.get())
    }
}
