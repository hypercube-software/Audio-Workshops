package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFArpSegment extends KFProgramSegment {
    private int tag; // loKey might be the tag if it's 26 bytes and follows the pattern
    private int hiKey;
    private int initialState;
    private int latchMode;
    private int playOrder;
    private int glissando;
    private int tempoSource;
    private int onOffControl;
    private int clocksPerBeat;
    private int durationPerBeat;
    private int initialTempo;
    private int velocityMode;
    private int velocityFixed;
    private int velocityCtrl;
    private int noteShift;
    private int shiftLimit;
    private int limitOption;
    private int arpSyncFlags;
    private int rfu1;
    private int rfu2;
    private int rfu3;

    public KFArpSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
