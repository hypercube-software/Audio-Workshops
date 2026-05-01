package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFAsrSegment extends KFProgramSegment {
    private int tag;
    private int rfu1;
    private int trigger;
    private int flags; // hold, rept
    private int dtime;
    private int atime;
    private int rfu2;
    private int rtime;

    public KFAsrSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
