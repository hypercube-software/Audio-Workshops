package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import lombok.Getter;

@Getter
public class RiffInfoChunk extends RiffChunk {
    private final String value;

    public RiffInfoChunk(RiffChunk parent, String id, int contentStart, int contentSize, String value) {
        super(parent, id, contentStart, contentSize);
        this.value = value;
    }

}
