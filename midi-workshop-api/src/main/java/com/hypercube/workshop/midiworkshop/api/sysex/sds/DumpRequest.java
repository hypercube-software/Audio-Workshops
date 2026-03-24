package com.hypercube.workshop.midiworkshop.api.sysex.sds;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;

public record DumpRequest(int channel, int sampleId) implements SampleDumpStandardMessage {
    public static boolean matches(byte[] payload) {
        return (payload.length == 6) &&
                ((payload[0] & 0xFF) == 0xF0) &&
                ((payload[1] & 0xFF) == 0x7E) &&
                ((payload[3] & 0xFF) == 0x03) &&
                ((payload[payload.length - 1] & 0xFF) == 0xF7);
    }

    public static DumpRequest fromBytes(byte[] payload) {
        return new DumpRequest(payload[2], MidiEventBuilder.from14Bits(payload, 4, true));
    }

    public CustomMidiEvent getRequest() {
        return MidiEventBuilder.parse("F0 7E %02X 03 %s F7".formatted(channel, MidiEventBuilder.to14Bits(sampleId, true)))
                .getFirst();
    }
}
