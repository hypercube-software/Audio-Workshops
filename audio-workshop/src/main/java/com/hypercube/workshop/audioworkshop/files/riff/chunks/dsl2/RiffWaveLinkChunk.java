package com.hypercube.workshop.audioworkshop.files.riff.chunks.dsl2;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

@Getter
public class RiffWaveLinkChunk extends RiffChunk {
    final short fusOptions;
    final short usPhaseGroup;
    final int ulChannel;
    final int sampleIndex;

    public RiffWaveLinkChunk(RiffChunk parent, String id, int contentStart, int contentSize, short fusOptions, short usPhaseGroup, int ulChannel, int sampleIndex) {
        super(parent, id, contentStart, contentSize);
        this.fusOptions = fusOptions;
        this.usPhaseGroup = usPhaseGroup;
        this.ulChannel = ulChannel;
        this.sampleIndex = sampleIndex;
    }
}
