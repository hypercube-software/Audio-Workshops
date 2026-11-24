package com.hypercube.workshop.midiworkshop.api.sysex.util;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class SysExReader {
    private final ByteBuffer buffer;

    public static List<byte[]> splitSysEx(byte[] input) {
        byte delimiter = (byte) 0xF0;
        List<byte[]> result = new ArrayList<>();
        if (input == null) {
            return result;
        }

        int start = -1;

        for (int i = 0; i < input.length; i++) {
            if (input[i] == delimiter) {
                if (start != -1) {
                    result.add(Arrays.copyOfRange(input, start, i));
                }
                start = i;
            }
        }

        if (start != -1) {
            result.add(Arrays.copyOfRange(input, start, input.length));
        }

        return result;
    }

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

    /**
     * Read 14 bit short packed into 2 Midi bytes
     */
    public int getInt14() {
        return unpack14bitValue(buffer.getShort());
    }

    /**
     * Read unpacked short (2 bytes)
     */
    public int getInt16() {
        return buffer.getShort();
    }

    /**
     * Read a 7-bit packed integer using {@code byteCount} MIDI bytes (1 to 4).
     * <p>Bytes are read most significant first, as in SysEx addresses.
     */
    public int getPackedInt(int byteCount) {
        if (byteCount < 1 || byteCount > 4) {
            throw new IllegalArgumentException("A packed MIDI integer must use between 1 and 4 bytes, got " + byteCount);
        }
        int value = 0;
        for (int i = 0; i < byteCount; i++) {
            value = (value << 7) | (buffer.get() & 0x7F);
        }
        return value;
    }

    /**
     * Read 21 bits (3 * 7) integer packed into 3 MIDI bytes
     */
    public int getInt21() {
        return getPackedInt(3);
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

    public String getString() {
        String str = "";
        while (remaining() > 0) {
            int c = getByte();
            if (c == 0) {
                break;
            }
            str += (char) c;
        }
        return str;
    }

    private int unpack14bitValue(int value) {
        return ((value & 0x7F00) >>> 1) | (value & 0x7F);
    }
}
