package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format

/**
 * Large documents, the same in every run, of at least the bytes asked for: orders as each format writes them, the
 * events of a log, and the rows of a table. What Faz 14 measures a document of 10 and of 50 MB by.
 */
internal object LargeSamples {
    private val Cities = listOf("İzmir", "London", "Arlington", "Wilmslow", "Hampton", "Helsinki", "Paoli", "Rotterdam")
    private val Names = listOf("Elif Yılmaz", "Ada Lovelace", "Grace Hopper", "Alan Turing", "Katherine Johnson", "Linus Torvalds")
    private val Statuses = listOf("shipped", "paid", "open", "cancelled")
    private val Events = listOf("open", "search", "format", "convert", "copy")

    /** The formats a large document is measured in. */
    val formats = listOf(Format.Json, Format.Ndjson, Format.Csv, Format.Xml, Format.Yaml, Format.Toml)

    /** A document of [format] of at least [bytes] bytes of UTF-8. */
    fun of(format: Format, bytes: Int): String = when (format) {
        Format.Json -> json(bytes)
        Format.Ndjson -> ndjson(bytes)
        Format.Csv -> csv(bytes)
        Format.Xml -> xml(bytes)
        Format.Yaml -> yaml(bytes)
        Format.Toml -> toml(bytes)
        else -> error("$format is not measured")
    }

    private fun total(index: Int) = "${(index * 37) % 1000}.${(index * 13) % 90 + 10}"

    private fun day(index: Int) = "2026-${(index % 12 + 1).toString().padStart(2, '0')}-${(index % 28 + 1).toString().padStart(2, '0')}"

    private fun json(bytes: Int) = buildString {
        append("{\n  \"orders\": [\n")
        var index = 0
        while (length < bytes) {
            if (index > 0) append(",\n")
            val name = Names[index % Names.size]
            append("    {\n")
            append("      \"id\": \"ord_${index.toString().padStart(7, '0')}\",\n")
            append("      \"status\": \"${Statuses[index % Statuses.size]}\",\n")
            append("      \"createdAt\": \"${day(index)}T14:32:00Z\",\n")
            append("      \"total\": ${total(index)},\n")
            append("      \"paid\": ${index % 3 != 0},\n")
            append("      \"coupon\": ${if (index % 5 == 0) "\"WELCOME\"" else "null"},\n")
            append("      \"customer\": { \"name\": \"$name\", \"city\": \"${Cities[index % Cities.size]}\" },\n")
            append("      \"items\": [\n")
            append("        { \"sku\": \"TS-${index % 100}\", \"qty\": ${index % 4 + 1}, \"price\": 39.95 },\n")
            append("        { \"sku\": \"MG-${index % 50}\", \"qty\": 1, \"price\": 12.50 }\n")
            append("      ]\n    }")
            index++
        }
        append("\n  ]\n}\n")
    }

    private fun ndjson(bytes: Int) = buildString {
        var index = 0
        while (length < bytes) {
            append(
                "{\"at\": \"${day(
                    index
                )}T09:${(index % 60).toString().padStart(2, '0')}:12Z\", \"event\": \"${Events[index % Events.size]}\", "
            )
            append("\"bytes\": ${1200 + index % 9000}, \"user\": \"${Names[index % Names.size]}\"}\n")
            index++
        }
    }

    private fun csv(bytes: Int) = buildString {
        append("id,name,city,total,paid,created\n")
        var index = 0
        while (length < bytes) {
            append("$index,${Names[index % Names.size]},${Cities[index % Cities.size]},${total(index)},${index % 3 != 0},${day(index)}\n")
            index++
        }
    }

    private fun xml(bytes: Int) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<orders>\n")
        var index = 0
        while (length < bytes) {
            append("  <order id=\"ord_$index\" status=\"${Statuses[index % Statuses.size]}\">\n")
            append("    <created>${day(index)}</created>\n    <total currency=\"EUR\">${total(index)}</total>\n")
            append("    <customer><name>${Names[index % Names.size]}</name><city>${Cities[index % Cities.size]}</city></customer>\n")
            append("    <item sku=\"TS-${index % 100}\" qty=\"${index % 4 + 1}\"/>\n  </order>\n")
            index++
        }
        append("</orders>\n")
    }

    private fun yaml(bytes: Int) = buildString {
        append("orders:\n")
        var index = 0
        while (length < bytes) {
            append("  - id: ord_$index\n    status: ${Statuses[index % Statuses.size]}\n    created: \"${day(index)}\"\n")
            append("    total: ${total(index)}\n    paid: ${index % 3 != 0}\n")
            append("    customer:\n      name: ${Names[index % Names.size]}\n      city: ${Cities[index % Cities.size]}\n")
            append("    items:\n      - sku: TS-${index % 100}\n        qty: ${index % 4 + 1}\n")
            index++
        }
    }

    private fun toml(bytes: Int) = buildString {
        append("title = \"Orders\"\n\n")
        var index = 0
        while (length < bytes) {
            append("[[orders]]\nid = \"ord_$index\"\nstatus = \"${Statuses[index % Statuses.size]}\"\ncreated = ${day(index)}\n")
            append("total = ${total(index)}\npaid = ${index % 3 != 0}\n")
            append("customer = { name = \"${Names[index % Names.size]}\", city = \"${Cities[index % Cities.size]}\" }\n\n")
            index++
        }
    }
}
