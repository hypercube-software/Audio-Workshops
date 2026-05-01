package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFLfoSegment extends KFProgramSegment {
    private int tag;
    private int rfu1;
    private int rateCtl;
    private int minRate;
    private int maxRate;
    private int phase;
    private int shape;
    private int rfu2;

    public KFLfoSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
