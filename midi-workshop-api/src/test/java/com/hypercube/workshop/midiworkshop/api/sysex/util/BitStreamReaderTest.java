package com.hypercube.workshop.midiworkshop.api.sysex.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BitStreamReaderTest {
    @Test
    void testBitStreamReader() throws Exception {
        byte[] data = {0b01110111, 0b00001111, 0b01010100};
        BitStreamReader bsr = new BitStreamReader(data);
        assertEquals(0b01, bsr.readBits(2));
        assertEquals(0b110, bsr.readBits(3));
        assertEquals(0b11, bsr.readBits(2));
        assertEquals(0b01010100000011110, bsr.readLsbBits(17));
    }

    @Test
    void testReadLsbBitsByteAligned() {
        byte[] data = {(byte) 0b10110010, 0b00001111};
        BitStreamReader bsr = new BitStreamReader(data);
        // bits 0..6 of 0xB2, with bit 0 first
        assertEquals(0b0110010, bsr.readLsbBits(7));
        // bit 7 of the first byte
        assertEquals(0b1, bsr.readLsbBits(1));
        // bits 0..6 of 0x0F
        assertEquals(0b0001111, bsr.readLsbBits(7));
    }
}
