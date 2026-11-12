package com.hypercube.workshop.midiworkshop.api.sysex.util;

public class BitStreamReader {

    private byte[] data;
    private int byteIndex;
    private int bitIndex;
    private int currentByte;

    public BitStreamReader(byte[] data) {
        this.data = data;
        setBitPos(0);
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

    public int getBitPos() {
        return byteIndex * 8 + bitIndex;
    }

    public void setBitPos(int bitPos) {
        this.byteIndex = bitPos / 8;
        this.bitIndex = bitPos % 8;
        this.currentByte = byteIndex < data.length ? (data[byteIndex] & 0xFF) : 0;
    }

    public void reset() {
        setBitPos(0);
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

    public int readShort() {
        return readBits(16);
    }

    public int readSignedShort() {
        return (short) readBits(16);
    }

    public int readSignedInt32() {
        return readBits(32);
    }

    public long readUnsignedInt32() {
        return readBits(32) & 0xFFFFFFFFL;
    }

    public int readByte() {
        return readBits(8);
    }

    public void skipBytes(int size) {
        if (size != 0) {
            readBits(size * 8);
        }
    }

    /**
     * Read bits in LSB-first order: the first bit read becomes bit 0 of the value.
     * <p>
     * This is the natural way to read little-endian (LSB first) fields.
     *
     * @param numBits number of bits to read
     * @return the reconstructed value
     */
    public int readLsbBits(int numBits) {
        if (numBits <= 0) {
            throw new RuntimeException("Number of bits to read must be positive.");
        }
        int value = 0;
        for (int b = 0; b < numBits; b++) {
            value = (readLsbBit() << b) | value;
        }
        return value;
    }

    /**
     * Read one bit in LSB-first order (bit 0 first, then bit 1, etc.).
     *
     * @return the bit value
     */
    public int readLsbBit() {
        if (bitIndex == 8) {
            bitIndex = 0;
            byteIndex++;
            currentByte = byteIndex < data.length ? data[byteIndex] : 0;
        }
        int bitValue = (currentByte >> bitIndex) & 0x1;
        bitIndex++;
        return bitValue;
    }

    /**
     * Read bits from MSB to LSB: the first bit read becomes the most significant bit
     * of the value.
     *
     * @return the bit value
     */
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


    public int getBytePos() {
        return getBitPos() / 8;
    }

    public byte[] readBytes(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) readByte();
        }
        return data;
    }

}
