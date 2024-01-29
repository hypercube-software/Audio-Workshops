package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import lombok.Getter;

@Getter
public class RiffAdtlLabelChunk extends RiffChunk {
    private final int cuePointId;
    private final String value;

    public RiffAdtlLabelChunk(RiffChunk parent, String id, int contentStart, int contentSize, int cuePointId, String value) {
        super(parent, id, contentStart, contentSize);
        this.cuePointId = cuePointId;
        this.value = value;
    }
}
