package io.amichne.konditional.uispec

@JvmInline
value class UiNodeId(val value: String)

@JvmInline
value class UiActionId(val value: String)

@JvmInline
value class JsonPointer(val value: String)

sealed interface UiValue {
    val kind: UiValueKind
}

enum class UiValueKind {
    NULL,
    BOOLEAN,
    STRING,
    INT,
    DOUBLE,
    ENUM,
    OBJECT,
    ARRAY,
    MAP,
    JSON,
}

data object UiNull : UiValue {
    override val kind: UiValueKind = UiValueKind.NULL
}

data class UiBoolean(val value: Boolean) : UiValue {
    override val kind: UiValueKind = UiValueKind.BOOLEAN
}

data class UiString(val value: String) : UiValue {
    override val kind: UiValueKind = UiValueKind.STRING
}

data class UiInt(val value: Long) : UiValue {
    override val kind: UiValueKind = UiValueKind.INT
}

data class UiDouble(val value: Double) : UiValue {
    override val kind: UiValueKind = UiValueKind.DOUBLE
}

data class UiEnum(val value: String) : UiValue {
    override val kind: UiValueKind = UiValueKind.ENUM
}

data class UiObject(val value: Map<String, UiValue>) : UiValue {
    override val kind: UiValueKind = UiValueKind.OBJECT
}

data class UiArray(val value: List<UiValue>) : UiValue {
    override val kind: UiValueKind = UiValueKind.ARRAY
}

data class UiMap(val value: Map<String, UiValue>) : UiValue {
    override val kind: UiValueKind = UiValueKind.MAP
}

data class UiJson(val value: String) : UiValue {
    override val kind: UiValueKind = UiValueKind.JSON
}
