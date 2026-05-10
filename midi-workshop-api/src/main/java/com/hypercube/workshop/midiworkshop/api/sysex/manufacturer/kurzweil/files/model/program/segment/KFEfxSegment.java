package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "chan", "prog", "mix", "ctl1", "out1", "ctl2", "out2"})
public class KFEfxSegment extends KFProgramSegment {
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
