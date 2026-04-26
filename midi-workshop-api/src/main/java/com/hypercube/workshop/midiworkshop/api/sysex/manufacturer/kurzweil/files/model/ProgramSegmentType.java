package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import java.util.Arrays;
import java.util.Optional;

public enum ProgramSegmentType {
    PGMSEGTAG(8),
    LYRSEGTAG(9),
    FXSEGTAG(15),
    ASRSEGTAG(16),
    LFOSEGTAG(20),
    FUNSEGTAG(24),
    ENCSEGTAG(32),
    ENVSEGTAG(33),
    IMPSEGTAG(39),
    CALSEGTAG(64),
    HOBSEGTAG(80),
    KDFXSEGTAG(104),
    KB3SEGTAG(120);

    private final short tag;

    ProgramSegmentType(int tag) {
        this.tag = (short) tag;
    }

    public static Optional<ProgramSegmentType> fromTag(int tag) {
        return Arrays.stream(ProgramSegmentType.values())
                .filter(e -> e.tag == tag || e.tag == (tag & 0xf8))
                .findFirst();
    }

    public short getTag() {
        return tag;
    }
}
