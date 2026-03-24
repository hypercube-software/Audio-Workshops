package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import lombok.Getter;

@Getter
public class ObjectScreenText extends BaseObject {
    private final String text;

    public ObjectScreenText(int objectType, int objectId, int size, String text) {
        super(objectType, objectId, size);
        this.text = text;
    }
}
