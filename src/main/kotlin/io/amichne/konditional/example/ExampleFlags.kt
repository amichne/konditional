package io.amichne.konditional.example

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp

object ExampleFlags {
    init {
        config {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(value = false)
                boundary {
                    platforms(Platform.IOS)
                    versions {
                        min(7, 10, 0)
                    }
                    note("US iOS staged rollout")
                    rampUp = RampUp.of(50.0)
                } implies true
                boundary {
                    locales(AppLocale.HI_IN)
                    note("IN Hindi full")
                } implies true
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(value = true, coverage = 100.0)
                boundary {
                    platforms(Platform.ANDROID)
                    versions {
                        max(6, 4, 99)
                    }
                    note("Android legacy off")
                } implies false
            }
        }
    }
}
