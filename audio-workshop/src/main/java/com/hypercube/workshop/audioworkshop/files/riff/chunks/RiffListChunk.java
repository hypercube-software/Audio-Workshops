package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl.Adtl;
import lombok.Getter;
import lombok.Setter;

@Getter
public class RiffListChunk extends RiffChunk {
    private final String listType;
    @Setter
    private Adtl adtl;

    public RiffListChunk(RiffChunk parent, String id, String listType, int contentStart, int contentSize) {
        super(parent, id, contentStart, contentSize);
        this.listType = listType;
    }

    @Override
    public String toString() {
        return getId() + " " + listType;
    }
}
