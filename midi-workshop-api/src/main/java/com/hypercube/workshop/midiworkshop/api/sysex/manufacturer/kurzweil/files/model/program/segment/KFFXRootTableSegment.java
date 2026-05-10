package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * <pre>
 * typedef struct {
 *      byte	tag;	// = fxrtSegTag
 *      byte	vers;
 *      word	studio;
 *      byte	rfu[4];
 *      fxptb	patch[numFXPatch];
 *      asrb	asr1, asr2;
 *      fcnb	fcn1, fcn2;
 *      lfob	lfo1, lfo2;
 *      fcnb	fcn3, fcn4;
 * } fxrtb;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "vers", "studio", "rfu", "patch", "asr1", "asr2", "fcn1", "fcn2", "lfo1", "lfo2", "fcn3", "fcn4"})
public class KFFXRootTableSegment extends KFProgramSegment {
    private int vers;
    private int studio;
    private int[] rfu;
    private List<KFFXPartSegment> patch;/* 18 FMOD */
    private KFAsrSegment asr1, asr2;
    private KFFcnSegment fcn1, fcn2;
    private KFLfoSegment lfo1, lfo2;
    private KFFcnSegment fcn3, fcn4;

    public KFFXRootTableSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
