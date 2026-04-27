package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * A Program is made of multiple segments called "blocks".
 * <ul>
 *     <li>They are identified with a "tag".</li>
 *     <li>Block tags are defined in the official spec in "segtag.h"</li>
 *     <li> Sizes have been computed by Gemini</li>
 * </ul>
 *
 * <p>
 * This is undocumented: they add sometimes an instance id to the tag
 * this mean:
 * <ul>
 *     <li>20 = first LFOSEGTAG</li>
 *     <li>21 = second LFOSEGTAG</li>
 *     <li>22 = third LFOSEGTAG</li>
 *     <li>...</li>
 * </ul>
 * This is why we need a mask 0xF8
 */
@Getter
public enum ProgramSegmentType {
    MASTSEGTAG(1, 0xFF, 72),  // mdb.h (Master parameters)
    CHANSEGTAG(2, 0xFF, 16),  // cdb.h (MIDI Channel parameters)
    ZONESEGTAG(3, 0xFF, 16),  // zdb.h (Zone parameters)
    CLOCKSEGTAG(4, 0xFF, 26), // not sure: arpb.h (Arpeggiator/Clock)
    PGMSEGTAG(8, 0xFF, 16),   // pgmb.h (Program Common)
    LYRSEGTAG(9, 0xFF, 16),   // lyrb.h (Layer Common)
    EFXSEGTAG(15, 0xFF, 8),   // efxb.h (Effect Control)
    ASRSEGTAG(16, 0xF8, 8),   // asrb.h (ASR envelopes)
    LFOSEGTAG(20, 0xF8, 8),   // lfob.h (LFOs)
    FCNSEGTAG(24, 0xF8, 4),   // fcnb.h (FUNs / Functions)
    ENCSEGTAG(32, 0xF8, 16),  // encb.h (Envelope Control)
    ENVSEGTAG(33, 0xFF, 16),  // envb.h (Envelope Segments)
    CALSEGTAG(64, 0xF8, 32),  // csegb.h (Calvin / Pitch / Sample)
    HOBSEGTAG(80, 0xF8, 16);  // hsegb.h (Hobbes / DSP / Amp)

    private final short tag;
    private final int mask;
    private final int size;

    ProgramSegmentType(int tag, int mask, int size) {
        this.tag = (short) tag;
        this.mask = mask;
        this.size = size;
    }

    public static Optional<ProgramSegmentType> fromTag(int realTag) {
        return Arrays.stream(ProgramSegmentType.values())
                .filter(e -> (realTag & e.mask) == e.tag)
                .findFirst();
    }
}
