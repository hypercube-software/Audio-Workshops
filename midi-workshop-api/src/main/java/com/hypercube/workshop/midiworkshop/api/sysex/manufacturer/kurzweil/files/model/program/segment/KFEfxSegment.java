package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFEfxSegment extends KFProgramSegment {
    private int tag;
    private int chan;
    private int prog;
    private int mix;
    private int ctl1;
    private int out1;
    private int ctl2;
    private int out2;

    public KFEfxSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
