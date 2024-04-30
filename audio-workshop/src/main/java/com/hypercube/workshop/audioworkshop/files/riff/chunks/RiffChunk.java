package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import com.hypercube.workshop.audioworkshop.files.exceptions.IncorrectRiffChunkParentSize;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class RiffChunk {
    private final String id;
    private final List<RiffChunk> children = new ArrayList<>();
    @Setter
    private int contentStart;
    @Setter
    private int contentSize; // does not include final padding
    @Setter
    private RiffChunk parent;

    public RiffChunk(RiffChunk parent, String id, int contentStart, int contentSize) {
        this.parent = parent;
        this.contentStart = contentStart;
        this.contentSize = contentSize;
        this.id = id;
        if (parent != null) {
            parent.addChild(this);
        }
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
        if (child.getContentEnd() > getContentEnd()) {
            throw new IncorrectRiffChunkParentSize(child, String.format("Child %s is outside the chunk %s", child.getId(), getId()));
        }
        children.add(child);
        child.setParent(this);
    }

    public List<RiffChunk> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public String toString() {
        return id;
    }

    public Optional<RiffChunk> getChunk(String chunkId) {
        assert (chunkId.length() == 4);
        return children.stream()
                .filter(c -> chunkIdEquals(chunkId, c))
                .findFirst();
    }

    public List<RiffChunk> getChunks(String chunkId) {
        assert (chunkId.length() == 4);
        return children.stream()
                .filter(c -> chunkIdEquals(chunkId, c))
                .toList();
    }

    /**
     * Test the id against the chunk id or the RiffListChunk id
     */
    private boolean chunkIdEquals(String chunkId, RiffChunk c) {
        assert (chunkId.length() == 4);
        return c.getId()
                .equals(chunkId) || (c instanceof RiffListChunk && ((RiffListChunk) c).getListType()
                .equals(chunkId));
    }

}
