package com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

import java.util.List;

@Getter
public class RiffPoolTableChunk extends RiffChunk {
    final List<Long> sampleOffsets;

    public RiffPoolTableChunk(RiffChunk parent, String id, int contentStart, int contentSize, List<Long> sampleOffsets) {
        super(parent, id, contentStart, contentSize);
        this.sampleOffsets = sampleOffsets;
    }
}
