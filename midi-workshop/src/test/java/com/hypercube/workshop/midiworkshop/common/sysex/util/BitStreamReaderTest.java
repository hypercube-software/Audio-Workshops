package com.hypercube.workshop.midiworkshop.common.sysex.util;

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
        assertEquals(0b00101010111100001, bsr.readInvertedBits(17));
    }
}