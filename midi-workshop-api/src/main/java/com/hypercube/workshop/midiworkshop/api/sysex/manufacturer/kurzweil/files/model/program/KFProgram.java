package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class KFProgram extends KFObject {
    private final List<KFProgramSegment> segments;
    private final int segmentsStart;
    private String name;

    public KFProgram(RawData data, int objectId, String name, int segmentsStart, List<KFProgramSegment> segments) {
        super(data, KObject.PROGRAM, objectId);
        this.name = name;
        this.segmentsStart = segmentsStart;
        this.segments = segments;
    }

    @JsonIgnore
    public byte[] getSegmentContent() {
        return Arrays.copyOfRange(data.content(), segmentsStart, data.content().length);
    }
}
