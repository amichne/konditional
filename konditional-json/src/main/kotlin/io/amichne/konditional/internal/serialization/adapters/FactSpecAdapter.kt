package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import io.amichne.konditional.context.fact.FactSpec

/** Each expression has exactly one operator. Operand strings are decoded by the namespace's fact domain. */
internal object FactSpecAdapter : JsonAdapter<FactSpec>() {
    override fun fromJson(reader: JsonReader): FactSpec {
        reader.beginObject()
        val operator = reader.nextName()
        val expression = when (operator) {
            "all" -> FactSpec.All(readTerms(reader))
            "anyOf" -> FactSpec.AnyOf(readTerms(reader))
            "not" -> FactSpec.Not(fromJson(reader))
            "named" -> readNamed(reader)
            "oneOf" -> {
                val fields = reader.readFields("fact", "operands")
                val operands = fields["operands"] as? List<*> ?: invalid("Expected operand array", reader)
                FactSpec.OneOf(fields.string("fact", reader), operands.map { it as? String ?: invalid("Expected string operand", reader) })
            }
            "atLeast", "atMost" -> {
                val fields = reader.readFields("fact", "operand")
                val fact = fields.string("fact", reader)
                val operand = fields.string("operand", reader)
                if (operator == "atLeast") FactSpec.AtLeast(fact, operand) else FactSpec.AtMost(fact, operand)
            }
            else -> invalid("Unknown fact operator '$operator'", reader)
        }
        reader.endObject()
        return expression
    }

    private fun readTerms(reader: JsonReader): List<FactSpec> = buildList {
        reader.beginArray()
        while (reader.hasNext()) add(fromJson(reader))
        reader.endArray()
    }

    private fun readNamed(reader: JsonReader): FactSpec.Named {
        var label: String? = null
        var term: FactSpec? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (val field = reader.nextName()) {
                "label" -> label = reader.readJsonValue() as? String ?: invalid("Expected string 'label'", reader)
                "term" -> term = fromJson(reader)
                else -> invalid("Unknown field '$field'", reader)
            }
        }
        reader.endObject()
        return FactSpec.Named(
            label ?: invalid("Expected fields label, term", reader),
            term ?: invalid("Expected fields label, term", reader),
        )
    }

    private fun JsonReader.readFields(vararg names: String): Map<*, *> {
        val fields = readJsonValue() as? Map<*, *> ?: invalid("Expected fact predicate object", this)
        if (fields.keys != names.toSet()) invalid("Expected fields ${names.joinToString()}", this)
        return fields
    }

    private fun Map<*, *>.string(name: String, reader: JsonReader): String =
        this[name] as? String ?: invalid("Expected string '$name'", reader)

    private fun invalid(reason: String, reader: JsonReader): Nothing =
        throw JsonDataException("$reason at ${reader.path}")

    override fun toJson(writer: JsonWriter, value: FactSpec?) {
        requireNotNull(value) { "Expected a fact expression" }
        writer.beginObject()
        when (value) {
            is FactSpec.All -> writeTerms(writer.name("all"), value.terms)
            is FactSpec.AnyOf -> writeTerms(writer.name("anyOf"), value.terms)
            is FactSpec.Not -> toJson(writer.name("not"), value.term)
            is FactSpec.Named -> {
                writer.name("named").beginObject()
                writer.name("label").value(value.label)
                toJson(writer.name("term"), value.term)
                writer.endObject()
            }
            is FactSpec.OneOf -> {
                writer.name("oneOf").beginObject()
                writer.name("fact").value(value.fact)
                writer.name("operands").beginArray()
                value.operands.forEach { writer.value(it) }
                writer.endArray().endObject()
            }
            is FactSpec.AtLeast -> writeBound(writer.name("atLeast"), value.fact, value.operand)
            is FactSpec.AtMost -> writeBound(writer.name("atMost"), value.fact, value.operand)
        }
        writer.endObject()
    }

    private fun writeTerms(writer: JsonWriter, terms: List<FactSpec>) {
        writer.beginArray()
        terms.forEach { toJson(writer, it) }
        writer.endArray()
    }

    private fun writeBound(writer: JsonWriter, fact: String, operand: String) {
        writer.beginObject()
        writer.name("fact").value(fact)
        writer.name("operand").value(operand)
        writer.endObject()
    }
}
