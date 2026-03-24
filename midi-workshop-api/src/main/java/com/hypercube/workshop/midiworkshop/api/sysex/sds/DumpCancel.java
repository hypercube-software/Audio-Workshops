package com.hypercube.workshop.midiworkshop.api.sysex.sds;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;

public record DumpCancel(int channel, int packedId) implements SampleDumpStandardMessage {
    public static boolean matches(byte[] payload) {
        return (payload.length == 6) &&
                ((payload[0] & 0xFF) == 0xF0) &&
                ((payload[1] & 0xFF) == 0x7E) &&
                ((payload[3] & 0xFF) == 0x7D) &&
                ((payload[payload.length - 1] & 0xFF) == 0xF7);
    }

    public static DumpCancel fromBytes(byte[] payload) {
        return new DumpCancel(payload[2], payload[4]);
    }

    public CustomMidiEvent getRequest() {
        return MidiEventBuilder.parse("F0 7E %02X 7D %02X F7".formatted(channel, packedId))
                .getFirst();
    }
}
