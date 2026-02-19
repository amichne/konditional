file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigValue.kt
package=io.amichne.konditional.serialization.instance
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.types.Konstrained,io.amichne.konditional.serialization.SchemaValueCodec,io.amichne.konditional.serialization.internal.toPrimitiveMap,io.amichne.kontracts.schema.ObjectTraits
type=io.amichne.konditional.serialization.instance.ConfigValue|kind=interface|decl=sealed interface ConfigValue
type=io.amichne.konditional.serialization.instance.BooleanValue|kind=class|decl=data class BooleanValue internal constructor(val value: Boolean) : ConfigValue
type=io.amichne.konditional.serialization.instance.StringValue|kind=class|decl=data class StringValue internal constructor(val value: String) : ConfigValue
type=io.amichne.konditional.serialization.instance.IntValue|kind=class|decl=data class IntValue internal constructor(val value: Int) : ConfigValue
type=io.amichne.konditional.serialization.instance.DoubleValue|kind=class|decl=data class DoubleValue internal constructor(val value: Double) : ConfigValue
type=io.amichne.konditional.serialization.instance.EnumValue|kind=class|decl=data class EnumValue internal constructor( val enumClassName: String, val constantName: String, ) : ConfigValue
type=io.amichne.konditional.serialization.instance.DataClassValue|kind=class|decl=data class DataClassValue internal constructor( val dataClassName: String, val fields: Map<String, Any?>, ) : ConfigValue
type=io.amichne.konditional.serialization.instance.KonstrainedPrimitive|kind=class|decl=data class KonstrainedPrimitive internal constructor( val konstrainedClassName: String, val rawValue: Any, ) : ConfigValue
type=io.amichne.konditional.serialization.instance.Opaque|kind=class|decl=data class Opaque internal constructor( val typeName: String, val debug: String, ) : ConfigValue
