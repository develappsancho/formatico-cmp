package com.developersancho.formatico.core.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatDetectorTest {
    @Test
    fun theFileNameIsBelievedFirst() {
        assertEquals(Format.Json, FormatDetector.detect("not json at all", fileName = "broken.JSON"))
        assertEquals(Format.Yaml, FormatDetector.detect("{}", fileName = "config.yml"))
        assertEquals(Format.Ndjson, FormatDetector.detect("", fileName = "events.jsonl"))
    }

    @Test
    fun theJsonFamily() {
        assertEquals(Format.Json, FormatDetector.detect("""  {"a": [1, 2]}"""))
        assertEquals(Format.Json, FormatDetector.detect("""{"a": """))
        assertEquals(Format.Jsonc, FormatDetector.detect("{\n  // a note\n  \"a\": 1\n}"))
        assertEquals(Format.Jsonc, FormatDetector.detect("[1, 2,]"))
        assertEquals(Format.Json5, FormatDetector.detect("{a: 'b'}"))
        assertEquals(Format.Ndjson, FormatDetector.detect("{\"id\": 1}\n{\"id\": 2}\n\n[3]\n"))
    }

    @Test
    fun xml() {
        assertEquals(Format.Xml, FormatDetector.detect("\uFEFF<?xml version=\"1.0\"?><a/>"))
        assertEquals(Format.Xml, FormatDetector.detect("\n  <root><item/></root>"))
    }

    @Test
    fun yaml() {
        assertEquals(Format.Yaml, FormatDetector.detect("---\nname: Formatico\n"))
        assertEquals(Format.Yaml, FormatDetector.detect("name: Formatico\nformats:\n  - json\n  - yaml\n"))
    }

    @Test
    fun tomlIniAndProperties() {
        assertEquals(Format.Toml, FormatDetector.detect("[server]\nport = 8080\nhost = \"localhost\"\n"))
        assertEquals(Format.Toml, FormatDetector.detect("title = \"Formatico\"\nversion = 1\n"))
        assertEquals(Format.Ini, FormatDetector.detect("; settings\n[server]\nhost = localhost\nport=8080\n"))
        assertEquals(Format.Properties, FormatDetector.detect("# app\napp.name=Formatico\napp.version=1.0\n"))
    }

    @Test
    fun tables() {
        assertEquals(Format.Csv, FormatDetector.detect("name,email,age\nElif,elif@example.com,34\n"))
        assertEquals(Format.Csv, FormatDetector.detect("name;age\nElif;34\n"))
        assertEquals(Format.Tsv, FormatDetector.detect("name\tage\nElif\t34\n"))
    }

    @Test
    fun nothingToTell() {
        assertNull(FormatDetector.detect(""))
        assertNull(FormatDetector.detect(" \n\t "))
        assertNull(FormatDetector.detect("Just a sentence."))
    }
}
