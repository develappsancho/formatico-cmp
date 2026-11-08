package com.developersancho.formatico.core.schema

import io.github.optimumcode.json.schema.SchemaType

/**
 * A draft of JSON Schema, as a schema's `$schema` names it: Formatico validates with any of them.
 *
 * @property displayName the draft's name as people say it: `2020-12`, `draft-07`.
 */
enum class SchemaDraft(val displayName: String, internal val type: SchemaType, internal val uri: String) {
    Draft4("draft-04", SchemaType.DRAFT_4, "http://json-schema.org/draft-04/schema"),
    Draft6("draft-06", SchemaType.DRAFT_6, "http://json-schema.org/draft-06/schema"),
    Draft7("draft-07", SchemaType.DRAFT_7, "http://json-schema.org/draft-07/schema"),
    Draft201909("2019-09", SchemaType.DRAFT_2019_09, "https://json-schema.org/draft/2019-09/schema"),
    Draft202012("2020-12", SchemaType.DRAFT_2020_12, "https://json-schema.org/draft/2020-12/schema");

    companion object {
        /** The draft a schema that names none in its `$schema` is read as. */
        val Default: SchemaDraft = Draft202012

        /** The draft [uri] names, with its `#` or without, over http or https; null where it names none. */
        fun named(uri: String): SchemaDraft? =
            runCatching { SchemaType.find(uri) }.getOrNull()?.let { found -> entries.first { it.type == found } }
    }
}
