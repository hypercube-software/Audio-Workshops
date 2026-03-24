package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BaseObject {
    private final int objectType;
    private final int objectId;
    private final int size;
}
