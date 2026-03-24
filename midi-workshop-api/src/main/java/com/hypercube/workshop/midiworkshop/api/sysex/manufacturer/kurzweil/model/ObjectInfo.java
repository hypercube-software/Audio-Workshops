package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import lombok.Getter;


@Getter
public final class ObjectInfo extends BaseObject {
    private final boolean inRAM;
    private final String name;

    public ObjectInfo(int objectType, int objectId, int size, boolean inRAM, String name) {
        super(objectType, objectId, size);
        this.inRAM = inRAM;
        this.name = name;
    }
}
