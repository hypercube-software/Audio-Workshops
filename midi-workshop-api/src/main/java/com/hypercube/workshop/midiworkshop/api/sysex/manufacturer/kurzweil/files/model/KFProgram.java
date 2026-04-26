package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import lombok.Getter;

import java.util.List;

@Getter
public class KFProgram extends KFObject {
    private final List<KFProgramSegment> segments;

    public KFProgram(RawData data, int objectId, List<KFProgramSegment> segments) {
        super(data, KObject.PROGRAM, objectId);
        this.segments = segments;
    }
}
