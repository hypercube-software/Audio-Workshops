package com.hypercube.workshop.midiworkshop.api.sysex.sds;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;

public record DumpHeader(int channel, int sampleId, int bitDepth, int sampleFrequency, int sampleLength,
                         LoopType loopType,
                         int loopStart,
                         int loopEnd) implements SampleDumpStandardMessage {
    public static boolean matches(byte[] payload) {
        return (payload.length == 21) &&
                ((payload[0] & 0xFF) == 0xF0) &&
                ((payload[1] & 0xFF) == 0x7E) &&
                ((payload[3] & 0xFF) == 0x01) &&
                ((payload[payload.length - 1] & 0xFF) == 0xF7);
    }

    public static DumpHeader fromBytes(byte[] payload) {
        boolean lsbFirst = true;
        LoopType loopType = payload[19] == 1 ? LoopType.BACKWARD_FORWARD : LoopType.FORWARD_ONLY;
        int samplePeriodNs = MidiEventBuilder.from21Bits(payload, 7, lsbFirst);
        int sampleFrequency = (int) (1e9 / samplePeriodNs);
        return new DumpHeader(payload[2],
                MidiEventBuilder.from14Bits(payload, 4, lsbFirst),
                payload[6],
                sampleFrequency,
                MidiEventBuilder.from21Bits(payload, 10, lsbFirst),
                loopType,
                MidiEventBuilder.from21Bits(payload, 13, lsbFirst),
                MidiEventBuilder.from21Bits(payload, 16, lsbFirst)
        );
    }

    public CustomMidiEvent getRequest() {
        boolean lsbFirst = true;
        int samplePeriodNs = (int) (1e9 / sampleFrequency);
        int intLoopType = switch (loopType) {
            case FORWARD_ONLY -> 0;
            case BACKWARD_FORWARD -> 1;
        };
        return MidiEventBuilder.parse("F0 7E %02X 01 %s %02X %s %s %s %s %02X F7".formatted(channel,
                        MidiEventBuilder.to14Bits(sampleId, lsbFirst),
                        bitDepth,
                        MidiEventBuilder.to21Bits(samplePeriodNs, lsbFirst),
                        MidiEventBuilder.to21Bits(sampleLength, lsbFirst),
                        MidiEventBuilder.to21Bits(loopStart, lsbFirst),
                        MidiEventBuilder.to21Bits(loopEnd, lsbFirst),
                        intLoopType
                ))
                .getFirst();
    }
}
