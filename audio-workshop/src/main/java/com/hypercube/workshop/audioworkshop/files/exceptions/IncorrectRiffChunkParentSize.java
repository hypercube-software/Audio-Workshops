package com.hypercube.workshop.audioworkshop.files.exceptions;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;


@Getter
public class IncorrectRiffChunkParentSize extends RuntimeException {
    private final String chunkId;
    private final int contentStart;

    public IncorrectRiffChunkParentSize(RiffChunk chunk, String msg) {
        super(msg);
        chunkId = chunk.getId();
        contentStart = chunk.getContentStart();
    }
}
