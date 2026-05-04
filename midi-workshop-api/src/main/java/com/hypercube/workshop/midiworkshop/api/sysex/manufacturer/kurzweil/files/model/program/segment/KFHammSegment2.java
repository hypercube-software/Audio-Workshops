package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFHammSegment2 extends KFProgramSegment {
    public KFHammSegment2(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
