package com.hypercube.workshop.midiworkshop.api.sysex.parser;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDecodingKey;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ManufacturerSysExParserTest {
    private static final String REGULAR_DECODING_KEY = """
            0 G7 F7 E7 D7 C7 B7 A7
            0 A6 A5 A4 A3 A2 A1 A0
            0 B6 B5 B4 B3 B2 B1 B0
            0 C6 C5 C4 C3 C2 C1 C0
            0 D6 D5 D4 D3 D2 D1 D0
            0 E6 E5 E4 E3 E2 E1 E0
            0 F6 F5 F4 F3 F2 F1 F0
            0 G6 G5 G4 G3 G2 G1 G0
            """;

    @Test
    void unpackVenomPatchName() {
        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setDecodingKey(new MidiDeviceDecodingKey(10, 2, REGULAR_DECODING_KEY));

        // Real response to the Venom "Single PatchName" request:
        // F0 00 01 05 21 00 01 05 01 00 F7
        byte[] payload = HexFormat.of()
                .parseHex("F0000105217F020501000056656E6F6D6F75007320205CF7");

        byte[] unpacked = new ManufacturerSysExParser().unpackMidiBuffer(device, payload);

        // Expected unpacked memory, from the Venom documentation:
        // 56 65 6E 6F 6D 6F 75 73 20 20 00 00 00 00
        assertArrayEquals(
                HexFormat.of().parseHex("56656E6F6D6F7573202000000000"),
                unpacked);
        assertEquals("Venomous  ", new String(unpacked, 0, 10, StandardCharsets.US_ASCII));
    }
}
