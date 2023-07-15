package com.hypercube.workshop.audioworkshop.files.flac.meta;


import com.hypercube.workshop.audioworkshop.files.flac.FlacBlockType;

public class FlacApplicationMetadata extends FlacMetadata {
    public FlacApplicationMetadata(FlacBlockType type, byte[] block, boolean isLatest, String id) {
        super(type, block, isLatest);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private final String id;
}
