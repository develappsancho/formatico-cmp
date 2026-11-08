package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.files.PantrixFileType
import com.pantrixui.foundation.platform.PlatformType

/** The largest file a desktop opens: 50 MB, counted by the thousand as the platforms count it. */
private const val DESKTOP_MAX_FILE_SIZE = 50_000_000L

/** The largest file a phone, a tablet and a browser open: 10 MB. */
private const val MOBILE_MAX_FILE_SIZE = 10_000_000L

/** The largest file Formatico opens on [platform], by the owner's measure (2026-11-02). */
fun maxFileSize(platform: PlatformType): Long =
    if (platform == PlatformType.Desktop) DESKTOP_MAX_FILE_SIZE else MOBILE_MAX_FILE_SIZE

/** The kinds of file Formatico reads, for the picker and the drop zone: each of its formats, and plain text. */
val DocumentFileTypes: List<PantrixFileType> =
    Format.entries.map { PantrixFileType(mimeType = it.mimeType, extensions = it.extensions) } + PantrixFileType.Text

private val Format.mimeType: String
    get() = when (this) {
        Format.Json, Format.Jsonc -> "application/json"
        Format.Json5 -> "application/json5"
        Format.Ndjson -> "application/x-ndjson"
        Format.Xml -> "application/xml"
        Format.Yaml -> "application/yaml"
        Format.Toml -> "application/toml"
        Format.Csv -> "text/csv"
        Format.Tsv -> "text/tab-separated-values"
        Format.Properties -> "text/x-java-properties"
        Format.Ini -> "text/plain"
    }
