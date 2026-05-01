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
    MASTSEGTAG(1, 72),  // mdb.h (Master parameters)
    CHANSEGTAG(2, 16),  // cdb.h (MIDI Channel parameters)
    ZONESEGTAG(3, 16),  // zdb.h (Zone parameters)
    CLOCKSEGTAG(4, 26), // arpb.h (Arpeggiator/Clock)
    PGMSEGTAG(8, 16),   // pgmb.h (Program Common)
    LYRSEGTAG(9, 16),   // lyrb.h (Layer Common)
    EFXSEGTAG(15, 8),   // efxb.h (Effect Control)
    ASRSEGTAG(16, 8),   // asrb.h (ASR envelopes)
    LFOSEGTAG(20, 8),   // lfob.h (LFOs)
    FCNSEGTAG(24, 4),   // fcnb.h (FUNs / Functions)
    ENCSEGTAG(32, 16),  // encb.h (Envelope Control)
    ENVSEGTAG(33, 16),  // envb.h (Envelope Segments)
    CALSEGTAG(64, 32),  // csegb.h (Calvin / Pitch / Sample)
    HOBSEGTAG(80, 16),  // hsegb.h (Hobbes / DSP / Amp)
    HAMSEGTAG(104, 32), // hammsegb.h (hammond organ simulation parameter segment)
    FXRTSEGTAG(120, 32),// fxrtb.h (real-time effects parameters)
    UNKNOWN(152, 0);

    private final int tag;
    private final int size;

    ProgramSegmentType(int tag, int size) {
        this.tag = tag;
        this.size = size;
    }
}
