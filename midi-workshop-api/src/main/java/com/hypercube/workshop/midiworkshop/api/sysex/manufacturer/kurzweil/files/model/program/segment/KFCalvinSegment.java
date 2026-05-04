package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFCalvinSegment extends KFProgramSegment {
    private int subTag;
    private int trans;
    private int dtune;
    private int tkScale;
    private int tvScale;
    private int tcontrol;
    private int trange;
    private int skeymap;
    private int sroot;
    private int slegato;
    private int keymap;
    private int root;
    private int legato;
    private int tshift;
    private int rfu2;
    private int cpitch;
    private int fpitch;
    private int ckScale;
    private int cvScale;
    private int pcontrol;
    private int prange;
    private int pdepth;
    private int pmin;
    private int pmax;
    private int psource;
    private int ccr;
    private int bitfields; // pbmode
    private int alg;
    private int fineHz;

    public KFCalvinSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
