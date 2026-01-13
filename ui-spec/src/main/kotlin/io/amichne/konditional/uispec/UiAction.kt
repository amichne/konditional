package io.amichne.konditional.uispec

data class UiAction(
    val id: UiActionId,
    val label: UiText,
    val intent: UiActionIntent = UiActionIntent.PRIMARY,
    val mutations: List<UiMutation> = emptyList(),
    val confirm: UiConfirm? = null,
    val visibility: UiConditionGroup? = null,
    val enabled: UiConditionGroup? = null,
)

enum class UiActionIntent {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    QUIET,
}

data class UiConfirm(
    val title: UiText? = null,
    val body: UiText? = null,
    val confirmLabel: UiText? = null,
    val cancelLabel: UiText? = null,
)

sealed interface UiMutation {
    val kind: UiMutationKind
}

enum class UiMutationKind {
    PATCH,
    SET,
}

data class UiPatchMutation(
    val operations: List<UiPatchOperation>,
) : UiMutation {
    override val kind: UiMutationKind = UiMutationKind.PATCH
}

data class UiSetMutation(
    val target: JsonPointer,
    val value: UiValue,
) : UiMutation {
    override val kind: UiMutationKind = UiMutationKind.SET
}

sealed interface UiPatchOperation {
    val op: UiPatchOp
    val path: JsonPointer
}

enum class UiPatchOp {
    ADD,
    REMOVE,
    REPLACE,
    MOVE,
    COPY,
    TEST,
}

data class UiPatchAdd(
    override val path: JsonPointer,
    val value: UiValue,
) : UiPatchOperation {
    override val op: UiPatchOp = UiPatchOp.ADD
}

data class UiPatchRemove(
    override val path: JsonPointer,
) : UiPatchOperation {
    override val op: UiPatchOp = UiPatchOp.REMOVE
}

data class UiPatchReplace(
    override val path: JsonPointer,
    val value: UiValue,
) : UiPatchOperation {
    override val op: UiPatchOp = UiPatchOp.REPLACE
}

data class UiPatchMove(
    override val path: JsonPointer,
    val from: JsonPointer,
) : UiPatchOperation {
    override val op: UiPatchOp = UiPatchOp.MOVE
}

data class UiPatchCopy(
    override val path: JsonPointer,
    val from: JsonPointer,
) : UiPatchOperation {
    override val op: UiPatchOp = UiPatchOp.COPY
}

data class UiPatchTest(
    override val path: JsonPointer,
    val value: UiValue,
) : UiPatchOperation {
    override val op: UiPatchOp = UiPatchOp.TEST
}
