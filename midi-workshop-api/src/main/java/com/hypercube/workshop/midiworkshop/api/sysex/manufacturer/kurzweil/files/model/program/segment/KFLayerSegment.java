package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFLayerSegment extends KFProgramSegment {
    private int tag;
    private int loEnable;
    private int trans;
    private int tune;
    private int loKey;
    private int hiKey;
    private int vRange; // vMax, vMin, vRfu
    private int eSwitch;
    private int flags; // ignRels, ignSust, ignSost, ignSusp, atkHold, susHold
    private int moreFlags; // pwlDis, enbRev, opaque, stereo, channum, keyUp
    private int vTrig; // vt1Level, vt1Sense, vt2Level, vt2Sense
    private int hiEnable;
    private int dlyCtl;
    private int dlyMin;
    private int dlyMax;
    private int xfade;

    public KFLayerSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
