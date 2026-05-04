package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/*
typedef struct {
	pgmb	pgm;
	asrb	asr;
	fcnb	fcn1;
	lfob	lfo;
	fcnb	fcn2;
	efxb	efx;
	layerb	layer[1];
} progb;
 */
@Getter
@Setter
public class KFProgram extends KFObject {
    private final int segmentsStart;
    private final String name;
    private KFProgramCommon pgm;
    private KFAsrSegment asr;
    private KFFcnSegment fcn1;
    private KFLfoSegment lfo;
    private KFFcnSegment fcn2;
    private KFEfxSegment efx;
    private KFFXRootTable fxrt;
    private KFHammSegment1 hammseg1;
    private KFHammSegment2 hammseg2;
    private KFHammSegment3 hammseg3;
    private KFLayer layer;

    //private List<KFProgramSegment> segments;

    public KFProgram(RawData data, int objectId, String name, int segmentsStart) {
        super(data, KObject.PROGRAM, objectId);
        this.name = name;
        this.segmentsStart = segmentsStart;
    }

    @JsonIgnore
    public byte[] getSegmentContent() {
        return Arrays.copyOfRange(data.content(), segmentsStart, data.content().length);
    }
}
