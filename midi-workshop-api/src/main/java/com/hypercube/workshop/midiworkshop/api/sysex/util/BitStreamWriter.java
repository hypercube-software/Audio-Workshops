package com.hypercube.workshop.midiworkshop.api.sysex.util;

import java.io.ByteArrayOutputStream;

public class BitStreamWriter {

    private final ByteArrayOutputStream output;
    private int currentByte;
    private int bitIndex;

    public BitStreamWriter() {
        this.output = new ByteArrayOutputStream();
        reset();
    }

    public void reset() {
        this.output.reset();
        this.currentByte = 0;
        this.bitIndex = 0;
    }

    public void writeBits(int value, int numBits) {
        if (numBits <= 0) {
            throw new RuntimeException("Number of bits to write must be positive.");
        }
        for (int i = numBits - 1; i >= 0; i--) {
            writeBit((value >> i) & 1);
        }
    }

    public void writeInvertedBits(int value, int numBits) {
        if (numBits <= 0) {
            throw new RuntimeException("Number of bits to write must be positive.");
        }
        for (int i = 0; i < numBits; i++) {
            writeBit((value >> i) & 1);
        }
    }

    public void writeBit(int bit) {
        if (bit == 1) {
            currentByte |= (1 << (7 - bitIndex));
        }

        bitIndex++;
        if (bitIndex == 8) {
            output.write(currentByte);
            currentByte = 0;
            bitIndex = 0;
        }
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream tempOutput = new ByteArrayOutputStream();
        try {
            tempOutput.write(output.toByteArray());
            if (bitIndex > 0) {
                tempOutput.write(currentByte);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tempOutput.toByteArray();
    }

    public int getBitPos() {
        return (output.size() * 8) + bitIndex;
    }

    public int getBytePos() {
        return getBitPos() / 2;
    }

    public void writeBytes(byte[] value) {
        for (byte v : value) {
            writeByte(v);
        }
    }

    public void writeByte(int value) {
        writeBits(value, 8);
    }

    public void writeShort(int value) {
        writeBits(value, 16);
    }
}
