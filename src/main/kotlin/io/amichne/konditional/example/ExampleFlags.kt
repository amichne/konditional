package io.amichne.konditional.example

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.core.SampleFeatureEnum

object ExampleFlags {
    init {
        config {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS withRules {
                default(value = false)
                rule {
                    platforms(Platform.IOS)
                    versions {
                        atLeast(7, 10, 0)
                    }
                    value(true, coveragePct = 50.0)
                    note("US iOS staged rollout")
                }
                rule {
                    locales(AppLocale.HI_IN)
                    value(true)
                    note("IN Hindi full")
                }
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME withRules {
                default(value = true, coverage = 100.0)
                rule {
                    platforms(Platform.ANDROID)
                    versions {
                        atMost(6, 4, 99)
                    }
                    value(false, coveragePct = 100.0)
                    note("Android legacy off")
                }
            }
        }
    }
}
