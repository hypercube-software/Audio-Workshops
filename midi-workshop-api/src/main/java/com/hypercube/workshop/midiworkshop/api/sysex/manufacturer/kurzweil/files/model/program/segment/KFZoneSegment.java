package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "chan", "prog", "lokey", "hikey", "flags", "trans", "ctls"})
public class KFZoneSegment extends KFProgramSegment {
    private int chan;
    private int prog;
    private int lokey;
    private int hikey;
    private int flags; // xmode, xpchg, pwheel
    private int trans;
    private int[] ctls;

    public KFZoneSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
