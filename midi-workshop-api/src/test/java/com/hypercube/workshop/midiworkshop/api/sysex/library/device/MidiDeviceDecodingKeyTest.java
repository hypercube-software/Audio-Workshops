package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * In the key, B2 means "byte 1, bit 2" (0 = LSB/right, 7 = MSB/left).
 * So B2 goes here: 00000000 00000100
 * NOT here       : 00000000 00100000
 */
class MidiDeviceDecodingKeyTest {
    private static Stream<Arguments> provideBitOffsets() {
        // Since MIDI uses only 7 bits, all input indexes % 8 == 0 are unmapped.
        // The input stream is read from left to right.
        return Stream.of(
                Arguments.of(0, -1),
                Arguments.of(1, 6), // A6 = target bit 6
                Arguments.of(2, 5),
                Arguments.of(3, 4),
                Arguments.of(4, 3),
                Arguments.of(5, 2),
                Arguments.of(6, 1),
                Arguments.of(7, 0), // A0 = target bit 0
                Arguments.of(8, -1),
                Arguments.of(9, 13),
                Arguments.of(10, 12),
                Arguments.of(11, 11),
                Arguments.of(12, 10),
                Arguments.of(13, 9),
                Arguments.of(14, 8),
                Arguments.of(15, 7), // A7 = target bit 7
                Arguments.of(16, -1)
        );
    }

    @ParameterizedTest
    @MethodSource
    void provideBitOffsets(int inputBitOffset, int expectedTargetBitOffset) {
        // GIVEN
        MidiDeviceDecodingKey key = new MidiDeviceDecodingKey(7, 1, """
                0 A6 A5 A4 A3 A2 A1 A0
                0 B5 B4 B3 B2 B1 B0 A7
                0 C4 C3 C2 C1 C0 B7 B6
                0 D3 D2 D1 D0 C7 C6 C5
                0 E2 E1 E0 D7 D6 D5 D4
                0 F1 F0 E7 E6 E5 E4 E3
                0 G0 F7 F6 F5 F4 F3 F2
                0 G7 G6 G5 G4 G3 G2 G1
                """);

        // WHEN
        var actual = key.getMapping();

        // THEN
        assertEquals(expectedTargetBitOffset, actual.get(inputBitOffset));
    }
}
