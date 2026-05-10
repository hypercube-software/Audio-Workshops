package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "rfu1", "rateCtl", "minRate", "maxRate", "phase", "shape", "rfu2"})
public class KFLfoSegment extends KFProgramSegment {
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
