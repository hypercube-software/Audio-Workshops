package com.hypercube.workshop.midiworkshop.api.sysex.sds;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.XORChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;

import java.util.Arrays;
import java.util.HexFormat;

public record DumpPacket(int channel, int packetId, byte[] data, int checksum) implements SampleDumpStandardMessage {
    public static boolean matches(byte[] payload) {
        return ((payload[0] & 0xFF) == 0xF0) &&
                ((payload[1] & 0xFF) == 0x7E) &&
                ((payload[3] & 0xFF) == 0x02) &&
                ((payload[payload.length - 1] & 0xFF) == 0xF7);
    }

    public static DumpPacket fromBytes(byte[] payload) {
        byte[] packedContent = Arrays.copyOfRange(payload, 5, payload.length - 2);
        return new DumpPacket(payload[2],
                payload[4] & 0xFF,
                packedContent,
                payload[payload.length - 2] & 0xFF);
    }

    public CustomMidiEvent getRequest() {
        String hex = HexFormat.ofDelimiter(" ")
                .withUpperCase()
                .formatHex(data);
        return MidiEventBuilder.parse("F0 7E %02X 02 %02X %s %02X F7".formatted(channel, packetId,
                        hex,
                        checksum & 0x7F))
                .getFirst();
    }

    public boolean check() {
        XORChecksum c = new XORChecksum();
        c.update(0x7E);
        c.update(channel);
        c.update(0x02);
        c.update(packetId);
        for (byte datum : data) {
            c.update(datum);
        }
        int result = c.getValue();
        return checksum == result;
    }
}
