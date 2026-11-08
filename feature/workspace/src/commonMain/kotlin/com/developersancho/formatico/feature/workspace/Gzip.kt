package com.developersancho.formatico.feature.workspace

/**
 * How many bytes [text], written in UTF-8, comes to gzipped by the platform's own compressor: the JVM's, zlib on iOS,
 * and the browser's `CompressionStream`. Null where the platform cannot gzip it.
 */
internal expect suspend fun gzippedSize(text: String): Long?
