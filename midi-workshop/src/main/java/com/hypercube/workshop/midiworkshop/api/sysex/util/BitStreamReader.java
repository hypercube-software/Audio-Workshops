package com.hypercube.workshop.midiworkshop.api.sysex.util;

public class BitStreamReader {

    private byte[] data;
    private int byteIndex;
    private int bitIndex;
    private int currentByte;

    public int getBitPos() {
        return byteIndex * 8 + bitIndex;
    }

    public BitStreamReader(byte[] data) {
        this.data = data;
        reset();
    }

    public void reset() {
        this.byteIndex = 0;
        this.bitIndex = 0;
        this.currentByte = data[byteIndex];
    }

    /**
     * Read bits from MSB to LSB
     *
     * @param numBits number of bits to read
     * @return the reconstructed value
     */
    public int readBits(int numBits) {
        if (numBits <= 0) {
            throw new RuntimeException("Number of bits to read must be positive.");
        }
        int value = 0;
        for (int b = 0; b < numBits; b++) {
            value = (value << 1) | readBit();
        }
        return value;
    }

    public void skipBytes(int size) {
        readBits(size * 8);
    }

    /**
     * Read bits from LSB to MSB
     *
     * @param numBits number of bits to read
     * @return the reconstructed value
     */
    public int readInvertedBits(int numBits) {
        if (numBits <= 0) {
            throw new RuntimeException("Number of bits to read must be positive.");
        }
        int value = 0;
        for (int b = 0; b < numBits; b++) {
            value = (readBit() << b) | value;
        }
        return value;
    }

    public int readBit() {
        if (bitIndex == 8) {
            bitIndex = 0;
            byteIndex++;
            currentByte = byteIndex < data.length ? data[byteIndex] : 0;
        }
        int bitValue = (currentByte >> (7 - bitIndex)) & 0x1;
        bitIndex++;
        return bitValue;
    }

    /**
     * Convenient method to debug binary values
     *
     * @param value value to convert in binary
     * @param size  size of the binary value to compute
     * @return the binary representation of the value
     */
    public static String getBinaryString(int value, int size) {
        String padded = "00000000000000000" + Integer.toBinaryString(value);
        padded = padded.substring(padded.length() - size);
        return padded;
    }

    /**
     * Convenient method to debug 7 bit binary values in LSB first
     */
    public static String getBinary7Inverted(int value) {
        return getBinaryString(value, 7).chars()
                .mapToObj(c -> (char) c)
                .reduce("", (a, b) -> b + a, (a2, b2) -> b2 + a2);
    }

    /**
     * Convenient method to debug 7 bit binary values in MSB first
     */
    public static String getBinary7(int value) {
        return getBinaryString(value, 7);
    }
}
