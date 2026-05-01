package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFChannelSegment extends KFProgramSegment {
    private int tag;
    private int chan;
    private int nlyrs;
    private int flags; // enable, progLock
    private int prog;
    private int volume; // volume, volLock
    private int pan; // pan, panLock
    private int trans;
    private int dtune;
    private int brange;
    private int playflags; // mono, port
    private int portRate;
    private int outflags; // output, hroom
    private int rfu1;
    private int rfu2;

    public KFChannelSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
