package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;

public enum Format {
    NIBBLE,
    STREAM;

    public static Format fromCode(int code) {
        return switch (code) {
            case 0 -> NIBBLE;
            case 1 -> STREAM;
            default -> throw new MidiError("Unexpected format code: %02X".formatted(code));
        };
    }
}
