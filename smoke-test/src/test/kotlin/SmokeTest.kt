import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enableWhen
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SmokeTest {
    private enum class Platform { IOS, ANDROID }

    private object ClientFacts : FactSchema() {
        val platform = single("client.os", FactDomain.enumeration(Platform.entries) { it.name })
    }

    private object Flags : Namespace<ClientFacts>("smoke", ClientFacts) {
        val enabled by boolean<Context>(default = false) {
            enableWhen { platform.oneOf(Platform.IOS) }
        }
    }

    private fun device(os: Platform) = Context(Facts.of(ClientFacts.platform.value(os)))

    @Test
    fun fullLifecycle() {
        assertTrue(Flags.enabled.evaluate(device(Platform.IOS)))

        val json = Flags.toJson()
        val result = Flags.fromJson(json)

        assertIs<ParseResult.Success<*>>(result)
        assertTrue(Flags.enabled.evaluate(device(Platform.IOS)))
        assertFalse(Flags.enabled.evaluate(device(Platform.ANDROID)))
    }
}
