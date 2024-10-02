package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.playlist;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RiffPlaylistChunk extends RiffChunk {
    private List<PlaylistSegment> segments = new ArrayList<>();

    public RiffPlaylistChunk(RiffChunk parent, String id, int contentStart, int contentSize) {
        super(parent, id, contentStart, contentSize);
    }

    public void addSegment(PlaylistSegment segment) {
        segments.add(segment);
    }

}
