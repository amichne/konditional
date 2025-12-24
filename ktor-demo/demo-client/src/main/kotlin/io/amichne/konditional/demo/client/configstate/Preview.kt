package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.configstate.Json.stableJson

internal fun renderSnapshotPreview(
    layout: Layout,
    state: LoadedState,
) {
    layout.snapshotPreview.value = stableJson(state.draftSnapshot)
}
