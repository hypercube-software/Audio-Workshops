package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.StreamFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import lombok.Getter;

@Getter
public class ObjectWrite extends BaseObject {
    private final int mode;
    private final String name;
    private final StreamFormat format;
    private final byte[] payload;
    private final KFObject kfObject;

    public ObjectWrite(int objectType, int objectId, int size, int mode, String name, StreamFormat format, byte[] payload, KFObject kfObject) {
        super(objectType, objectId, size);
        this.mode = mode;
        this.name = name;
        this.format = format;
        this.payload = payload;
        this.kfObject = kfObject;
    }
}
