package io.amichne.konditional.core.types

import io.amichne.konditional.core.types.json.JsonValue

@SubclassOptInRequired
interface JsonEncodeable<J : JsonValue> : EncodableValue<J>
