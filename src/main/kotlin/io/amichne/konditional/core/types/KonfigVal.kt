package io.amichne.konditional.core.types

import kotlin.reflect.KClass

sealed interface KonfigVal<T : Any> {


    enum class Encoding(val klazz: KClass<*>) {
        BOOLEAN(Boolean::class),
        STRING(String::class),
        INTEGER(Int::class),
        DECIMAL(Double::class),
    }

    val value: T
    val encoding: Encoding

    data class Decimal(override val value: Double) : KonfigVal<Double> {
        override val encoding: Encoding = Encoding.DECIMAL
    }
    data class Integer(override val value: Int) : KonfigVal<Int> {
        override val encoding: Encoding = Encoding.INTEGER
    }

    data class Str(override val value: String) : KonfigVal<String> {
        override val encoding: Encoding = Encoding.STRING
    }

    data class Bool(override val value: Boolean) : KonfigVal<Boolean> {
        override val encoding: Encoding = Encoding.BOOLEAN
    }
}
