package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFZoneSegment extends KFProgramSegment {
    private int tag;
    private int chan;
    private int prog;
    private int lokey;
    private int hikey;
    private int flags; // xmode, xpchg, pwheel
    private int trans;
    private int[] ctls = new int[8];

    public KFZoneSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
