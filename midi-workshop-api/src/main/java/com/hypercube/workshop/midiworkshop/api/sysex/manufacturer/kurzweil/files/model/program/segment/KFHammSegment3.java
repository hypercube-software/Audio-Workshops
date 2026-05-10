package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * typedef	struct	{
 * 	byte	segtag3;	// = hammSegTag3
 * 	byte	drawbars[9];
 * 	byte    rfu6;
 * 	byte    rfu7;
 * 	ubyte   keyclick;	// now a switch
 * 	ubyte   relclick;	// now a switch
 * 	byte    noizran;
 * 	byte    rfu4;
 * 	byte    loTune;
 * 	byte	rcvMap;         // 0 = Kurzweil, 1 = Voce
 * 	byte	rfu5[14];
 * } hammsegb_seg3;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "drawbars", "rfu6", "rfu7", "keyclick", "relclick", "noizran", "rfu4", "loTune", "rcvMap", "rfu5"})
public class KFHammSegment3 extends KFProgramSegment {
    private int[] drawbars = new int[9];
    private int rfu6;
    private int rfu7;
    private int keyclick;
    private int relclick;
    private int noizran;
    private int rfu4;
    private int loTune;
    private int rcvMap;
    private int[] rfu5 = new int[14];

    public KFHammSegment3(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
