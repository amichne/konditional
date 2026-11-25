package io.amichne.konditional.engine

import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.fixtures.TestNamespace
import io.amichne.konditional.fixtures.Platform
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.fixtures.TestContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluationSnapshotTest {
    @Test
    fun reloadDuringDeferredRuleKeepsDependencyAndDiagnosticsOnOriginalVersion() {
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val flags = object : TestNamespace("evaluation-snapshot") {
            val dependency by integer<Context>(default = 0)
            val composed by integer<Context>(default = 0) {
                ruleResolved({ platform.oneOf(Platform.IOS) }) {
                    entered.countDown()
                    check(resume.await(10, TimeUnit.SECONDS))
                    dependency()
                }
            }
        }
        fun configuration(value: Int) = Configuration(
            flags = mapOf(flags.dependency to FlagDefinition(defaultValue = value, feature = flags.dependency)),
            metadata = ConfigurationMetadata(version = "v$value"),
        )

        flags.load(configuration(1))
        val context = TestContext(platform = Platform.IOS)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val pending = executor.submit<Pair<Int, String?>> {
                flags.composed.explain(context).let { it.value to it.configVersion }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            flags.load(configuration(2))
            resume.countDown()
            assertEquals(1 to "v1", pending.get(10, TimeUnit.SECONDS))
            assertEquals(2 to "v2", flags.composed.explain(context).let { it.value to it.configVersion })
        } finally {
            resume.countDown()
            executor.shutdownNow()
        }
    }
}
