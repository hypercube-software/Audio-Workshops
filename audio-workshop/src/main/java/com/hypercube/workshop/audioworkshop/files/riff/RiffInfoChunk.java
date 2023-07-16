package com.hypercube.workshop.audioworkshop.files.riff;

import lombok.Getter;

@Getter
public class RiffInfoChunk extends RiffChunk {
    private final String value;

    public RiffInfoChunk(String id, int contentStart, int contentSize, String value) {
        super(id, contentStart, contentSize);
        this.value = value;
    }

}
