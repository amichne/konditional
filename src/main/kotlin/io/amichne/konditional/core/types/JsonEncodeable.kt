package io.amichne.konditional.core.types

import io.amichne.kontracts.value.JsonValue

@SubclassOptInRequired
interface JsonEncodeable<J : JsonValue> : EncodableValue<J>
