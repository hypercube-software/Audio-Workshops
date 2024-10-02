package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

@Getter
public class RiffAdtlTextChunk extends RiffChunk {
    private final CuePointLabeledText cuePointLabeledText;

    public RiffAdtlTextChunk(RiffChunk parent, String id, int contentStart, int contentSize, CuePointLabeledText cuePointLabeledText) {
        super(parent, id, contentStart, contentSize);
        this.cuePointLabeledText = cuePointLabeledText;
    }

}
