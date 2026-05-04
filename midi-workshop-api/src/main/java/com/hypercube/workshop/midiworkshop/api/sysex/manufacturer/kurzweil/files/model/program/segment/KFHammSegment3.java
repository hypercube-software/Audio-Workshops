package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFHammSegment3 extends KFProgramSegment {
    public KFHammSegment3(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
