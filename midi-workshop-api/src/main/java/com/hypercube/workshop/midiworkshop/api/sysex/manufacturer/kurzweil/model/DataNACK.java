package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import lombok.Getter;


@Getter
public final class DataNACK extends BaseObject {
    private final int offset;

    public DataNACK(int objectType, int objectId, int offset, int size) {
        super(objectType, objectId, size);
        this.offset = offset;
    }
}
