package io.amichne.konditional.demo.client.configstate

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

internal data class LoadedState(
    val response: dynamic,
    val draftSnapshot: dynamic,
    val selectedFlagIndex: Int?,
    val flagFilter: String,
    val lastSavedSnapshotJson: String,
)

internal data class Layout(
    val frameEl: HTMLDivElement,
    val titleEl: HTMLElement,
    val subtitleEl: HTMLElement,
    val dirtyBadge: HTMLElement,
    val refreshBtn: HTMLButtonElement,
    val resetBtn: HTMLButtonElement,
    val saveBtn: HTMLButtonElement,
    val advancedBtn: HTMLButtonElement,
    val statusEl: HTMLDivElement,
    val searchInput: HTMLInputElement,
    val flagCount: HTMLElement,
    val flagList: HTMLDivElement,
    val editor: HTMLDivElement,
    val snapshotPreview: HTMLTextAreaElement,
    val toastHost: HTMLDivElement,
)

internal sealed interface Status {
    val message: String

    data class Loading(
        override val message: String,
    ) : Status

    data class Ready(
        override val message: String,
    ) : Status

    data class Error(
        override val message: String,
    ) : Status
}
