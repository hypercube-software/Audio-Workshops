package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import lombok.Getter;

@Getter
public class RiffListChunk extends RiffChunk {
    private final String listType;

    public RiffListChunk(RiffChunk parent, String id, String listType, int contentStart, int contentSize) {
        super(parent, id, contentStart, contentSize);
        this.listType = listType;
    }

    @Override
    public String toString() {
        return getId() + " " + listType;
    }
}
