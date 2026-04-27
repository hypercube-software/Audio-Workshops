package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import lombok.Getter;

@Getter
public class KFProgramSegment {
    private final RawData segmentContent;
    private final ProgramSegmentType type;

    public KFProgramSegment(RawData segmentContent, ProgramSegmentType type) {
        this.segmentContent = segmentContent;
        this.type = type;
    }
}
