package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KFKeyMap;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * typedef struct {
 * 	pgmb	pgm;
 * 	asrb	asr;
 * 	fcnb	fcn1;
 * 	lfob	lfo;
 * 	fcnb	fcn2;
 * 	efxb	efx;
 * 	layerb	layer[1];
 * } progb;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"type", "objectId", "name", "data", "segmentsStart", "pgm", "asr", "fcn1", "lfo", "fcn2", "efx", "fxrt", "hammseg1", "hammseg2", "hammseg3", "layers"})
public class KFProgram extends KFObject {
    private int segmentsStart;
    private KFProgramCommon pgm;
    private KFAsrSegment asr;
    private KFFcnSegment fcn1;
    private KFLfoSegment lfo;
    private KFFcnSegment fcn2;
    private KFEfxSegment efx;
    private KFFXRootTableSegment fxrt;
    private KFHammSegment1 hammseg1;
    private KFHammSegment2 hammseg2;
    private KFHammSegment3 hammseg3;
    private List<KFLayer> layers;

    public KFProgram(RawData data,
                     String name, int objectId,
                     int segmentsStart) {
        super(data, KObject.PROGRAM, name, objectId);
        this.segmentsStart = segmentsStart;
    }

    @JsonIgnore
    public byte[] getSegmentContent() {
        return Arrays.copyOfRange(data.getContent(), segmentsStart, data.getContent().length);
    }

    public boolean contains(KFKeyMap keymap) {
        return getLayers()
                .stream()
                .anyMatch(l -> l.getCseg()
                        .getKeymap() == keymap.getObjectId() || l.getCseg()
                        .getSkeymap() == keymap.getObjectId());
    }
}
