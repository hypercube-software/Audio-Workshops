package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

public record MidiControllerValue(int msb, int lsb) {
    public static MidiControllerValue fromValue(int value) {
        return new MidiControllerValue(value >> 7, value & 0x7F);
    }

    public int toValue() {
        return (msb << 7) | lsb;
    }
}
