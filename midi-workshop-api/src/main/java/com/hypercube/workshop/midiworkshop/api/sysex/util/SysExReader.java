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

    public int getInt16() {
        return buffer.getShort();
    }

    public int getInt24() {
        int b2 = buffer.get();
        int b1 = buffer.get();
        int b0 = buffer.get();
        return b0 | (b1 << 7) | (b2 << 14);
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
}
