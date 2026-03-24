package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import lombok.Getter;

import java.awt.image.BufferedImage;

@Getter
public class ObjectScreenBitmap extends BaseObject {
    private final BufferedImage image;

    public ObjectScreenBitmap(int objectType, int objectId, int size, BufferedImage image) {
        super(objectType, objectId, size);
        this.image = image;
    }
}
