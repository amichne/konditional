package io.amichne.konditional.fixtures.utilities

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform

fun localeIds(vararg locales: AppLocale): Set<String> = locales.mapTo(linkedSetOf()) { it.id }

fun platformIds(vararg platforms: Platform): Set<String> = platforms.mapTo(linkedSetOf()) { it.id }
