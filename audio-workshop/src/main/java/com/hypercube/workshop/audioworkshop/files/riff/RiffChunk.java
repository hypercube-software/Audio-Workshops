package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.files.exceptions.AudioParserException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class RiffChunk {
    private final int contentStart;
    private final String id;
    private final List<RiffChunk> children = new ArrayList<>();
    private int contentSize; // does not include final padding
    private RiffChunk parent;

    public RiffChunk(String id, int contentStart, int contentSize) {
        this.contentStart = contentStart;
        this.contentSize = contentSize;
        this.id = id;
    }


    public void setContentSize(int contentSize) {
        this.contentSize = contentSize;
    }


    public void setParent(RiffChunk parent) {
        this.parent = parent;
    }

    /**
     * The start position of the last byte of the chunk
     *
     * @return offset in the RIFF file
     */
    public int getContentEnd() {
        return contentStart + contentSize - 1;
    }

    /**
     * The start position of the first byte AFTER the chunk
     *
     * @return offset in the RIFF file
     */
    public int getChunkEnd() {
        return contentStart + contentSize;
    }

    public void addChild(RiffChunk child) {
        if (child.getContentEnd() > getContentEnd())
            throw new AudioParserException(String.format("Child %s is outside the chunk %s", child.getId(), getId()));
        children.add(child);
        child.setParent(this);
    }

    public List<RiffChunk> getChildren() {
        return Collections.unmodifiableList(children);
    }
}
