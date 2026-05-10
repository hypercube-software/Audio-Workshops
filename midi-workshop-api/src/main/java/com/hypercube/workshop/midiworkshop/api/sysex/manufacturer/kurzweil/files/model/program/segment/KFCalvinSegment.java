package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "subTag", "trans", "dtune", "tkScale", "tvScale", "tcontrol", "trange", "skeymap",
        "sroot", "slegato", "keymap", "root", "legato", "tshift", "rfu2", "cpitch", "fpitch", "ckScale", "cvScale", "pcontrol",
        "prange", "pdepth", "pmin", "pmax", "psource", "ccr", "alg", "fineHz"})
public class KFCalvinSegment extends KFProgramSegment {
    private int subTag;
    private int trans;
    private int dtune;
    private int tkScale;
    private int tvScale;
    private int tcontrol;
    private int trange;
    private int skeymap; // Object ID of the stereo Keymap
    private int sroot;
    private int slegato;
    private int keymap; // Object ID of the mono Keymap
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
    private int alg;
    private int fineHz;

    public KFCalvinSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
