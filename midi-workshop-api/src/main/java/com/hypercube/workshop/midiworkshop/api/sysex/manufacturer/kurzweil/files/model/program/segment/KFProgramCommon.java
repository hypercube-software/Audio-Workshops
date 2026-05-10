package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

/*
typedef	struct {
	ubyte	tag;
    ubyte	fmt;
    ubyte	numLayers;
    ubyte	modeFlags;
    byte	bendRange;
    ubyte	portSlope;
    byte	mixControl;
    byte	mixRange;
    byte	coarse1;
    byte	control1;
    byte	range1;
    byte	dest1;
    byte	coarse2;
    byte	control2;
    byte	range2;
    byte	dest2;
} pgmb;
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "fmt", "numLayers", "modeFlags", "bendRange", "portSlope", "mixControl", "mixRange",
        "coarse1", "control1", "range1", "dest1", "coarse2", "control2", "range2", "dest2"})

public class KFProgramCommon extends KFProgramSegment {
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
