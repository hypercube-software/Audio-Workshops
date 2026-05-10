package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * typedef struct {
 * 	byte	tag;	// = fxptSegTag
 * 	byte	num;	// which slot #
 * 	byte	strip;
 * 	byte	param;	// which param #
 * 	byte	adjust;
 * 	byte	source;
 * 	byte	depth;
 * 	byte	rfu;
 * } fxptb;
 *
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "num", "strip", "param", "adjust", "source", "depth", "rfu"})
public class KFFXPartSegment extends KFProgramSegment {
    private int num;
    private int strip;
    private int param;
    private int adjust;
    private int source;
    private int depth;
    private int rfu;

    public KFFXPartSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
