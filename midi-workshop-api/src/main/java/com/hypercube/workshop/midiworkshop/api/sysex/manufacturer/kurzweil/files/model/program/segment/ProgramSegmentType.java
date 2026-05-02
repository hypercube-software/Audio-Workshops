package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import lombok.Getter;

/**
 * A Program is made of multiple segments called "blocks".
 * <ul>
 *     <li>They are identified with a "tag".</li>
 *     <li>Block tags are defined in the official spec in "segtag.h"</li>
 *     <li>Most sizes have been computed by Gemini</li>
 * </ul>
 *
 * <p>
 * The tag id is not properly defined in the official .H, they are in fact "base tags"
 * #define	calSegTag	64
 * #define	hobSegTag	80
 * When you have "holes" like this, it means multiple calSeg can be found with tags 64,65,66,..
 * So a tag, no only gives you a segment type, but also a segment instance id !
 * the instance id is tagValue - baseTag: 64-66 = 2
 */
@Getter
public enum ProgramSegmentType {
    MASTSEGTAG(1, 72, "mdb"),  // mdb.h (Master parameters)
    CHANSEGTAG(2, 16, "cdb"),  // cdb.h (MIDI Channel parameters)
    ZONESEGTAG(3, 16, "zdb"),  // zdb.h (Zone parameters)
    CLOCKSEGTAG(4, 26, "arpb"), // arpb.h (Arpeggiator/Clock)
    PGMSEGTAG(8, 16, "pgmb"),   // pgmb.h (Program Common)
    LYRSEGTAG(9, 16, "lyrb"),   // lyrb.h (Layer Common)
    EFXSEGTAG(15, 8, "efxb"),   // efxb.h (Effect Control without KDFX)
    ASRSEGTAG(16, 8, "asrb"),   // asrb.h (ASR envelopes)
    LFOSEGTAG(20, 8, "lfob"),   // lfob.h (LFOs)
    FCNSEGTAG(24, 4, "fcnb"),   // fcnb.h (FUNs / Functions)
    ENCSEGTAG(32, 16, "encb"),  // encb.h (Envelope Control)
    ENVSEGTAG(33, 16, "envb"),  // envb.h (Envelope Segments)
    ATKSEGTAG(39, 16, "encb"),  // encb.h (Envelope Control)
    CALSEGTAG(64, 32, "csegb"),  // csegb.h (Calvin / Pitch / Sample)
    HOBSEGTAG(80, 16, "hsegb"),  // hsegb.h (Hobbes / DSP / Amp)
    FXRTSEGTAG(104, 8, "fxrtb"), // fxrtb.h (KDFX real-time effects parameters)
    FXPTSEGTAG(105, 8, "fxptb"), // fxrtb.h FXMOD
    HAMSEGTAG(120, 32, "hammsegb"), // 0x68 hammsegb.h (hammond organ simulation parameter segment)
    UNKNOWN(152, 0);

    private final int tag;
    private final int size;
    private final String orgName;

    ProgramSegmentType(int tag, int size, String orgName) {
        this.tag = tag;
        this.size = size;
        this.orgName = orgName;
    }

    ProgramSegmentType(int tag, int size) {
        this.tag = tag;
        this.size = size;
        this.orgName = "";
    }
}
