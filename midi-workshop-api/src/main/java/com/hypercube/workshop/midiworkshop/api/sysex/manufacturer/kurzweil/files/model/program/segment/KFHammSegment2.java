package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * typedef	struct	{
 * 	byte	segtag2;	// = hammSegTag2
 * 	byte	noizpitch;	// noise pitch
 * 	ubyte	noizlevel;	// noise level (dB/10)
 * 	ubyte	noizdecay;	// noise decay (enum)
 * 	byte	percmode:1;	// percussion off/on
 * 	byte	percharm:1;	// percussion low/high
 * 	byte	percfast:1;	// percussion slow/fast
 * 	byte	percloud:1;	// percussion soft/loud
 * 	byte	percbarT:4;	// percussion trigger harmonic #
 * 	byte	percbarL:4;	// low percussion harmonic #
 * 	byte	percbarH:4;	// high percussion harmonic #
 * 	ubyte	noizvel;
 * 	ubyte	percvel;
 * 	ubyte	perclevel[4];	// percussion levels (dB/10)
 * 	ubyte	percdecay[4];	// percussion decays (enum)
 * 	byte	perclcomp[4];	// loud/soft compensation (db/10)
 * 	ubyte	rfu2[4];
 * 	byte	bass_q, treb_q;	// additional eq params
 * 	ubyte	noizrtrig;	// noise re-trigger threshold
 * 	ubyte	leakmode;	// leakage mode
 * 	byte    leslieCtl;
 * 	byte    chorusCtl;
 * 	byte    chorusSelect;
 * 	byte	rfu3;
 * } hammsegb_seg2;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "noizpitch", "noizlevel", "noizdecay", "perc1", "perc2", "noizvel", "percvel",
        "perclevel", "percdecay", "perclcomp", "rfu2", "bass_q", "treb_q", "noizrtrig", "leakmode", "leslieCtl", "chorusCtl",
        "chorusSelect", "rfu3"})
public class KFHammSegment2 extends KFProgramSegment {
    private int noizpitch;
    private int noizlevel;
    private int noizdecay;
    private int perc1;
    private int perc2;
    private int noizvel;
    private int percvel;
    private int[] perclevel = new int[4];
    private int[] percdecay = new int[4];
    private int[] perclcomp = new int[4];
    private int[] rfu2 = new int[4];
    private int bass_q;
    private int treb_q;
    private int noizrtrig;
    private int leakmode;
    private int leslieCtl;
    private int chorusCtl;
    private int chorusSelect;
    private int rfu3;

    public KFHammSegment2(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
