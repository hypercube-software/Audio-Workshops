package com.hypercube.workshop.midiworkshop.api.sysex.util;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class SysExReader {
    private final ByteBuffer buffer;

    public int getByte() {
        return buffer.get() & 0xFF;
    }

    public int peekByte() {
        int position = buffer.position();
        int value = getByte();
        buffer.position(position);
        return value;
    }

    public int remaining() {
        return buffer.remaining();
    }

    public void mark() {
        buffer.mark();
    }

    public void reset() {
        buffer.reset();
    }

    public byte[] getBytes(int size) {
        byte[] data = new byte[size];
        buffer.get(data);
        return data;
    }

    public int getInt24() {
        int b2 = buffer.get();
        int b1 = buffer.get();
        int b0 = buffer.get();
        return b0 | (b1 << 8) | (b2 << 16);
    }


    public String readASCIIString(int size) {
        byte[] data = new byte[size];
        buffer.get(data);
        return new String(data, StandardCharsets.US_ASCII);
    }

    public void skip(int size) {
        IntStream.range(0, size)
                .forEach(i -> buffer.get());
    }

    public int position() {
        return buffer.position();
    }
}
