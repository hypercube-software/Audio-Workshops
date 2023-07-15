package com.hypercube.workshop.audioworkshop.files.flac.meta;

import com.hypercube.workshop.audioworkshop.files.flac.FlacBlockType;

public class FlacMetadata {
    final protected byte[] block;
    final protected boolean isLatest;
    final protected FlacBlockType type;

    public FlacMetadata(FlacBlockType type, byte[] block, boolean isLatest) {
        super();
        this.type = type;
        this.block = block;
        this.isLatest = isLatest;
    }

    public byte[] getBlock() {
        return block;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public FlacBlockType getType() {
        return type;
    }
}
