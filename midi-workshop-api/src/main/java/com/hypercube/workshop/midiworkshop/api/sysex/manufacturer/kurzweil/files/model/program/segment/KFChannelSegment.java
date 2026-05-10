package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "chan", "nlyrs", "flags", "prog", "volume", "pan", "trans", "dtune", "brange",
        "playflags", "portRate", "outflags", "rfu1", "rfu2"})
public class KFChannelSegment extends KFProgramSegment {
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
