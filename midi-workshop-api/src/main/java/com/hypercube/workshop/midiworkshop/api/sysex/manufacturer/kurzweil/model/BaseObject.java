package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BaseObject {
    private final int objectType;
    private final int objectId;
    private final int size;

    public String getObjectTypeHexa() {
        return "%04X".formatted(objectType);
    }

    public int get14BitsObjectType() {
        return MidiEventBuilder.from14bits(objectType, false);
    }
}
