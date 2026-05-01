package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFProgramCommon extends KFProgramSegment {
    private int tag;
    private int fmt;
    private int numLayers;
    private int modeFlags;
    private int bendRange;
    private int portSlope;
    private int mixControl;
    private int mixRange;
    private int coarse1;
    private int control1;
    private int range1;
    private int dest1;
    private int coarse2;
    private int control2;
    private int range2;
    private int dest2;

    public KFProgramCommon(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
