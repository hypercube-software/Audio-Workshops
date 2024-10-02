package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

@Getter
public class RiffAdtlLabelChunk extends RiffChunk {
    private final CuePointLabel cuePointLabel;

    public RiffAdtlLabelChunk(RiffChunk parent, String fieldId, int contentStart, int contentSize, CuePointLabel cuePointLabel) {
        super(parent, fieldId, contentStart, contentSize);
        this.cuePointLabel = cuePointLabel;
    }
}
