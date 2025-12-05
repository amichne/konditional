package io.amichne.konditional.fixtures.core

import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.kontext.AppLocale
import io.amichne.konditional.kontext.Kontext
import io.amichne.konditional.kontext.Platform
import io.amichne.konditional.kontext.Version

data class TestKontext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId
) : Kontext<TestNamespace>
