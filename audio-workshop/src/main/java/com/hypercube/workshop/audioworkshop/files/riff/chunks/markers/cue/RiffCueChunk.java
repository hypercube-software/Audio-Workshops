package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.cue;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RiffCueChunk extends RiffChunk {
    private List<CuePoint> cuePoints = new ArrayList<>();

    public RiffCueChunk(RiffChunk parent, String id, int contentStart, int contentSize) {
        super(parent, id, contentStart, contentSize);
    }

    public void addCuePoint(CuePoint cuePoint) {
        cuePoints.add(cuePoint);
    }

}
