package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl;

import java.util.List;

/**
 * Associated data list
 * <a href="https://www.recordingblogs.com/wiki/associated-data-list-chunk-of-a-wave-file">...</a>
 */
public record Adtl(
        List<RiffAdtlTextChunk> texts,
        List<RiffAdtlLabelChunk> labels) {
}
