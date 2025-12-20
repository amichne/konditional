package io.amichne.konditional.fixtures.utilities

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.values.LocaleTagIdValue
import io.amichne.konditional.values.PlatformTagIdValue

fun localeIds(vararg locales: AppLocale): Set<LocaleTagIdValue> =
    locales.mapTo(linkedSetOf()) { LocaleTagIdValue.from(it.id) }

fun platformIds(vararg platforms: Platform): Set<PlatformTagIdValue> =
    platforms.mapTo(linkedSetOf()) { PlatformTagIdValue.from(it.id) }
