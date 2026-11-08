package com.developersancho.formatico.feature.start

import formatico.feature.start.generated.resources.Res

/** Reads the text of a [Sample]. */
fun interface SampleSource {
    /** The text of [sample]. */
    suspend fun read(sample: Sample): String
}

/** The samples as this module's resources hold them, in `files/samples/`. */
internal class ResourceSampleSource : SampleSource {
    override suspend fun read(sample: Sample): String =
        Res.readBytes("files/samples/${sample.fileName}").decodeToString()
}
