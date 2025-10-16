package io.amichne.konditional.example

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.example.SampleFeatureEnum

object ExampleFlags {
    init {
        config {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS withRules {
                default(value = false)
                rule {
                    platforms(Platform.IOS)
                    version {
                        leftBound(7, 10, 0)
                    }
                    note("US iOS staged rollout")
                    rampUp = 50.0
                } gives true
                rule {
                    locales(AppLocale.HI_IN)
                    note("IN Hindi full")
                } gives true
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME withRules {
                default(value = true, coverage = 100.0)
                rule {
                    platforms(Platform.ANDROID)
                    version {
                        rightBound(6, 4, 99)
                    }
                    note("Android legacy off")
                } gives false
            }
        }
    }
}
