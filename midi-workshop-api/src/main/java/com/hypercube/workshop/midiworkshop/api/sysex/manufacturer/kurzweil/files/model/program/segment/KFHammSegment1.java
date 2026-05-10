package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * typedef	struct	{
 * 	byte	segtag1;	// = hammSegTag1
 * 	byte	version;
 * 	byte	basekey;	// key # of base tone wheel
 * 	byte	nwheels;	// # tone wheels
 * 	byte	toneswap:1;	// tone wheel hi/low swap
 * 	byte	nopreamp:1;	// preamp response disable
 * 	byte	perctrig:1;     // percussion retrigger
 * 	byte	livebars:1;	// draw bar enable
 * 	byte	barquant:1;	// draw bar quantization
 * 	byte	keynoise:1;	// noise wheel off/on
 * 	byte	noizmulti:1;	// noise wheel multi-trigger
 * 	byte	noizfiltr:1;	// use lopass filter for noise env
 * 	ubyte	toneleak;	// tone wheel leakage level
 * 	ubyte	resmap;		// keyboard resistor map #
 * 	byte	bartones[9];	// draw bar tuning (semis)
 * 	ubyte	volmap;		// tone wheel volume map #
 * 	byte	volume;		// tone wheel volume adjust
 * 	byte	balance;	// tone wheel hi/low balance
 * 	byte	emph_g, emph_f;	// preemphasis
 * 	byte	bass_g, bass_f;	// bass shelf
 * 	byte	par1_g, par1_f, par1_q;	// parametric
 * 	byte	par2_g, par2_f, par2_q;	// parametric
 * 	byte	treb_g, treb_f;	// treble shelf
 * 	byte	noizvol;	// noise volume adjust
 * } hammsegb_seg1;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "version", "basekey", "nwheels", "bitfields1", "toneleak", "resmap", "bartones", "volmap", "volume", "balance", "emph_g", "emph_f", "bass_g", "bass_f", "par1_g", "par1_f", "par1_q", "par2_g", "par2_f", "par2_q", "treb_g", "treb_f", "noizvol"})
public class KFHammSegment1 extends KFProgramSegment {
    private int version;
    private int basekey;
    private int nwheels;
    private int bitfields1; // Includes toneswap, nopreamp, perctrig, livebars, barquant, keynoise, noizmulti, noizfiltr
    private int toneleak;
    private int resmap;
    private int[] bartones = new int[9];
    private int volmap;
    private int volume;
    private int balance;
    private int emph_g, emph_f;
    private int bass_g, bass_f;
    private int par1_g, par1_f, par1_q;
    private int par2_g, par2_f, par2_q;
    private int treb_g, treb_f;
    private int noizvol;

    public KFHammSegment1(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
