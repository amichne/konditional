package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import kotlin.properties.ReadOnlyProperty

abstract class FlagModule<C : Context> {
    protected inline fun <reified T : Any> boolean(): ReadOnlyProperty<FlagModule<C>, Feature<Boolean, C>> {
        return
    }
}
