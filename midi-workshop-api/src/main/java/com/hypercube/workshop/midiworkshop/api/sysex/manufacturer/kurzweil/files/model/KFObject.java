package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class KFObject {
    private final RawData data;
    private final KObject type;
    private final int objectId;
}
