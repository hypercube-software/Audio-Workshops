package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "bitfields1", "scsiID", "bchan", "rvmap", "rpmap", "intTbl", "intKey", "sysxID",
        "trans", "dtune", "sampflags", "playflags", "rfu2", "samptime", "curSetup", "oldSetup", "curBank", "curEntry", "fxflags",
        "curEffect", "localKbdChan", "fxMix", "echan", "bitfields2", "curSong", "tvmap", "tpmap", "curDisk", "contrast", "view",
        "confirm", "xflags", "kbdTrans", "xvmap", "xpmap", "dchan", "bitfields3", "markList", "bitfields4", "bitfields5",
        "bitfields6", "bitfields7", "seqClickProg", "seqQuantGrid", "seqQuantAmt", "seqQuantSwing", "listIndex", "listTop", "seqTempo"})
public class KFMasterSegment extends KFProgramSegment {
    private int bitfields1; // seqTempoLock, displayLink, rANO, rpchg, rmode
    private int scsiID;
    private int bchan;
    private int rvmap;
    private int rpmap;
    private int intTbl;
    private int intKey;
    private int sysxID;
    private int trans;
    private int dtune;
    private int sampflags;
    private int playflags;
    private int rfu2;
    private int samptime;
    private int curSetup;
    private int oldSetup;
    private int curBank;
    private int curEntry;
    private int fxflags;
    private int curEffect;
    private int localKbdChan;
    private int fxMix;
    private int echan;
    private int bitfields2; // macroState, rfu3
    private int curSong;
    private int tvmap;
    private int tpmap;
    private int curDisk;
    private int contrast;
    private int view;
    private int confirm;
    private int xflags;
    private int kbdTrans;
    private int xvmap;
    private int xpmap;
    private int dchan;
    private int bitfields3; // seqRecDub, seqRecMode, seqLoop, seqSync
    private int[] markList = new int[10];
    private int bitfields4; // seqCountOff, seqClickMode, seqClickChan
    private int bitfields5; // seqClickKey, seqClock
    private int bitfields6; // seqClickVel, seqKeyWait
    private int bitfields7; // bootDisk, defaultDisk
    private int seqClickProg;
    private int seqQuantGrid;
    private int seqQuantAmt;
    private int seqQuantSwing;
    private int listIndex;
    private int listTop;
    private int seqTempo;

    public KFMasterSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
