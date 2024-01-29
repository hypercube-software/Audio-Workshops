package com.hypercube.workshop.audioworkshop.files.flac.meta;

import com.hypercube.workshop.audioworkshop.files.flac.FlacBlockType;
import lombok.Getter;

public class FlacMetadata {
    @Getter
    protected final byte[] block;
    protected final boolean isLatest;
    @Getter
    protected final FlacBlockType type;

    public FlacMetadata(FlacBlockType type, byte[] block, boolean isLatest) {
        super();
        this.type = type;
        this.block = block;
        this.isLatest = isLatest;
    }

    public boolean isLatest() {
        return isLatest;
    }

}
