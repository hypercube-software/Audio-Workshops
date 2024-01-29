package com.hypercube.workshop.audioworkshop.files.riff.chunks.gig;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

import java.util.List;

@Getter
public class RiffG3DimensionChunk extends RiffChunk {
    final List<G3Dimension> dimensions;

    public RiffG3DimensionChunk(RiffChunk parent, String id, int contentStart, int contentSize, List<G3Dimension> dimensions) {
        super(parent, id, contentStart, contentSize);
        this.dimensions = dimensions;
    }
}
