import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.SingleFact
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SmokeTest {
    private enum class Platform { IOS, ANDROID }

    @Test
    fun fullLifecycle() {
        val platform = SingleFact("client.os", FactDomain.enumeration(Platform.entries) { it.name })
        val flags = object : Namespace("smoke", factSchema = FactSchema(platform)) {
            val enabled by boolean<Context>(default = false) {
                rule(true, platform.oneOf(Platform.IOS))
            }
        }
        fun device(os: Platform) = object : Context {
            override val facts = Facts.of(platform.value(os))
        }

        assertTrue(flags.enabled.evaluate(device(Platform.IOS)))

        val json = flags.toJson()
        val result = flags.fromJson(json)

        assertIs<ParseResult.Success<*>>(result)
        assertTrue(flags.enabled.evaluate(device(Platform.IOS)))
        assertFalse(flags.enabled.evaluate(device(Platform.ANDROID)))
    }
}
