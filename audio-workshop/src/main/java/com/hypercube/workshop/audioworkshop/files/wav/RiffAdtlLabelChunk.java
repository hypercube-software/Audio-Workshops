package com.hypercube.workshop.audioworkshop.files.wav;

import lombok.Getter;

@Getter
public class RiffAdtlLabelChunk extends RiffChunk {
    private final int cuePointId;
    private final String value;

    public RiffAdtlLabelChunk(String id, int contentStart, int contentSize, int cuePointId, String value) {
        super(id, contentStart, contentSize);
        this.cuePointId = cuePointId;
        this.value = value;
    }
}
