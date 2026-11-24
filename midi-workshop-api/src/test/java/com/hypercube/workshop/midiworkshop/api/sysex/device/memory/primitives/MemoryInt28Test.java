package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryInt28Test {

    @Test
    void parsesTwoByteAddress() {
        MemoryInt28 value = MemoryInt28.from("0128", true);

        assertEquals(0x00A8, value.value());
        assertEquals(2, value.byteCount());
        assertArrayEquals(new int[]{0x01, 0x28}, value.getPackedBytes());
    }

    @Test
    void parsesThreeByteAddress() {
        MemoryInt28 value = MemoryInt28.from("000128", true);

        assertEquals(0x00A8, value.value());
        assertEquals(3, value.byteCount());
        assertArrayEquals(new int[]{0x00, 0x01, 0x28}, value.getPackedBytes());
    }

    @Test
    void parsesFourByteAddress() {
        MemoryInt28 value = MemoryInt28.from("00000128", true);

        assertEquals(0x00A8, value.value());
        assertEquals(4, value.byteCount());
        assertArrayEquals(new int[]{0x00, 0x00, 0x01, 0x28}, value.getPackedBytes());
    }

    @Test
    void parsesMaximumFourByteValue() {
        MemoryInt28 value = MemoryInt28.from("7F7F7F7F", true);

        assertEquals(0x0FFFFFFF, value.value());
        assertEquals(4, value.byteCount());
    }

    @Test
    void roundTripsEveryWidth() {
        for (int byteCount = 1; byteCount <= MemoryInt28.MAX_BYTE_COUNT; byteCount++) {
            int max = MemoryInt28.maxValue(byteCount);
            MemoryInt28 value = MemoryInt28.from(max, byteCount);

            int[] expected = new int[byteCount];
            Arrays.fill(expected, 0x7F);
            assertArrayEquals(expected, value.getPackedBytes());
            assertEquals(value, MemoryInt28.fromPacked(value.packedValue(), byteCount));
        }
    }

    @Test
    void arithmeticKeepsByteCount() {
        MemoryInt28 value = MemoryInt28.from("0128", true)
                .add(1);

        assertEquals(0x00A9, value.value());
        assertEquals(2, value.byteCount());
        assertEquals(2, value.getPackedBytes().length);
    }

    @Test
    void fromIntKeepsThreeBytesAndPromotesToFourOnlyWhenNeeded() {
        MemoryInt28 fitsInThree = MemoryInt28.from(MemoryInt28.maxValue(3));
        assertEquals(3, fitsInThree.byteCount());

        MemoryInt28 needsFour = MemoryInt28.from(MemoryInt28.maxValue(3) + 1);
        assertEquals(4, needsFour.byteCount());
        assertEquals(MemoryInt28.maxValue(3) + 1, needsFour.value());
    }

    @Test
    void rejectsOddHexLength() {
        assertThrows(MidiError.class, () -> MemoryInt28.from("128", true));
    }

    @Test
    void rejectsValueTooLargeForWidth() {
        assertThrows(MidiError.class, () -> MemoryInt28.from(1 << 14, 2));
    }

    @Test
    void rejectsInvalidPackedByte() {
        assertThrows(MidiError.class, () -> MemoryInt28.from("0180", true));
        assertThrows(MidiError.class, () -> MemoryInt28.fromPacked(0x0180, 2));
    }

    @Test
    void rejectsOverflowOnAdd() {
        MemoryInt28 max = MemoryInt28.from(MemoryInt28.maxValue(2), 2);

        assertThrows(MidiError.class, () -> max.add(1));
    }
}
