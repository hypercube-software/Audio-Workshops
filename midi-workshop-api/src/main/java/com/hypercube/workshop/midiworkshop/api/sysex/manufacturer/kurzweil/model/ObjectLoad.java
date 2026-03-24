package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.StreamFormat;
import lombok.Getter;

@Getter
public class ObjectLoad extends BaseObject {
    private final int offset;
    private final StreamFormat format;
    private final byte[] payload;

    public ObjectLoad(int objectType, int objectId, int size, int offset, StreamFormat format, byte[] payload) {
        super(objectType, objectId, size);
        this.offset = offset;
        this.format = format;
        this.payload = payload;
    }
}
