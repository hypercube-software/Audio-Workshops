package com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;

/**
 * Non-negative integer encoded as 1 to 4 MIDI 7-bit bytes.
 *
 * <p>MIDI SysEx only transports 7-bit values, so a memory address or a size is
 * split into {@code byteCount} bytes of 7 bits:
 * <ul>
 *     <li>1 byte  : 7 bits</li>
 *     <li>2 bytes : 14 bits</li>
 *     <li>3 bytes : 21 bits (classic Roland address)</li>
 *     <li>4 bytes : 28 bits (maximum)</li>
 * </ul>
 *
 * <p>Both representations are kept:
 * <ul>
 *     <li>{@link #value()}: the unpacked value, up to 28 bits</li>
 *     <li>{@link #packedValue()}: the MIDI-packed value, {@code byteCount} bytes,
 *         each byte in [0x00,0x7F]</li>
 * </ul>
 *
 * <p>The name {@code MemoryInt28} refers to the maximum unpacked width
 * (4 bytes * 7 bits). The actual width is stored in {@link #byteCount()}.
 *
 * <p>Examples with {@code @Packed}:
 * <pre>
 *   "0128"     -> 2 MIDI bytes, value 0x00A8
 *   "000128"   -> 3 MIDI bytes, value 0x00A8
 *   "00000128" -> 4 MIDI bytes, value 0x00A8
 * </pre>
 */
public record MemoryInt28(int value, int packedValue, int byteCount) {
    /**
     * Width used when no explicit width is available (historical Roland addresses).
     */
    public static final int DEFAULT_BYTE_COUNT = 3;
    /**
     * Maximum number of MIDI bytes: 4 * 7 = 28 bits.
     */
    public static final int MAX_BYTE_COUNT = 4;

    public MemoryInt28 {
        if (byteCount < 1 || byteCount > MAX_BYTE_COUNT) {
            throw new MidiError("A MIDI packed integer must use between 1 and %d bytes, got %d"
                    .formatted(MAX_BYTE_COUNT, byteCount));
        }
        if (value < 0 || value > maxValue(byteCount)) {
            throw new MidiError("Value 0x%X does not fit in %d MIDI bytes (max 0x%X)"
                    .formatted(value, byteCount, maxValue(byteCount)));
        }
        if (packedValue != pack(value, byteCount)) {
            throw new MidiError("Inconsistent packed value 0x%X for value 0x%X on %d MIDI bytes"
                    .formatted(packedValue, value, byteCount));
        }
    }

    /**
     * Parse a hexadecimal memory value.
     *
     * <p>When {@code packed} is true, the string is the MIDI wire representation:
     * each pair of hex digits is one 7-bit byte, so its length directly gives the
     * number of MIDI bytes. {@code "0128"} is a 2-byte value, {@code "000128"} a
     * 3-byte value, {@code "00000128"} a 4-byte value.
     *
     * @param value  hexadecimal string, even number of digits, 2 to 8
     * @param packed true if the string is the 7-bit packed representation
     * @return the parsed value, or null if {@code value} is null
     */
    public static MemoryInt28 from(String value, boolean packed) {
        if (value == null) {
            return null;
        }
        String hex = value.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.isEmpty()) {
            throw new MidiError("Empty memory value");
        }
        if ((hex.length() & 1) != 0) {
            throw new MidiError("Memory value must contain an even number of hexadecimal digits: " + value);
        }
        int byteCount = hex.length() / 2;
        if (byteCount < 1 || byteCount > MAX_BYTE_COUNT) {
            throw new MidiError("Memory value must use between 1 and %d MIDI bytes: %s"
                    .formatted(MAX_BYTE_COUNT, value));
        }
        try {
            if (packed) {
                int packedValue = 0;
                for (int i = 0; i < byteCount; i++) {
                    int b = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
                    if (b > 0x7F) {
                        throw new MidiError("Invalid packed MIDI byte 0x%02X in %s".formatted(b, value));
                    }
                    packedValue = (packedValue << 8) | b;
                }
                return fromPacked(packedValue, byteCount);
            }
            long parsed = Long.parseLong(hex, 16);
            if (parsed > maxValue(byteCount)) {
                throw new MidiError("Value 0x%X does not fit in %d MIDI bytes"
                        .formatted(parsed, byteCount));
            }
            return from((int) parsed, byteCount);
        } catch (NumberFormatException e) {
            throw new MidiError("Invalid hexadecimal memory value: " + value, e);
        }
    }

    /**
     * Build a value using the historical 3-byte width when the value fits,
     * otherwise promote to the 4-byte maximum.
     *
     * <p>This keeps all existing Roland values on 3 bytes (no change on the wire)
     * while allowing values that need 28 bits.
     */
    public static MemoryInt28 from(int unpackedValue) {
        int byteCount = unpackedValue <= maxValue(DEFAULT_BYTE_COUNT)
                ? DEFAULT_BYTE_COUNT
                : MAX_BYTE_COUNT;
        return from(unpackedValue, byteCount);
    }

    /**
     * @param byteCount number of MIDI bytes, between 1 and 4
     */
    public static MemoryInt28 from(int unpackedValue, int byteCount) {
        return new MemoryInt28(unpackedValue, pack(unpackedValue, byteCount), byteCount);
    }

    /**
     * Build a 3-byte value from its packed representation (historical default).
     */
    public static MemoryInt28 fromPacked(int packedValue) {
        return fromPacked(packedValue, DEFAULT_BYTE_COUNT);
    }

    /**
     * @param packedValue MIDI-packed value, each byte in [0x00,0x7F]
     * @param byteCount   number of MIDI bytes, between 1 and 4
     */
    public static MemoryInt28 fromPacked(int packedValue, int byteCount) {
        return new MemoryInt28(unpack(packedValue, byteCount), packedValue, byteCount);
    }

    /**
     * @return a zero value using the default 3-byte width
     */
    public static MemoryInt28 zero() {
        return from(0);
    }

    /**
     * Add an amount, keeping the original MIDI width.
     *
     * @throws MidiError if the result does not fit in the original width
     */
    public MemoryInt28 add(int amount) {
        return from(value + amount, byteCount);
    }

    /**
     * Add another value, keeping this value's MIDI width.
     */
    public MemoryInt28 add(MemoryInt28 amount) {
        return add(amount.value);
    }

    public MemoryInt28 div(int amount) {
        return from(value / amount, byteCount);
    }

    public MemoryInt28 mul(int amount) {
        return from(value * amount, byteCount);
    }

    /**
     * @return exactly {@link #byteCount()} MIDI bytes, most significant first
     */
    public int[] getPackedBytes() {
        int[] data = new int[byteCount];
        for (int i = 0; i < byteCount; i++) {
            data[byteCount - 1 - i] = (packedValue >> (8 * i)) & 0xFF;
        }
        return data;
    }

    /**
     * @return the maximum unpacked value for the given number of MIDI bytes
     */
    public static int maxValue(int byteCount) {
        if (byteCount < 1 || byteCount > MAX_BYTE_COUNT) {
            throw new MidiError("Invalid MIDI byte count: " + byteCount);
        }
        return (1 << (7 * byteCount)) - 1;
    }

    private static int pack(int value, int byteCount) {
        int packed = 0;
        for (int i = 0; i < byteCount; i++) {
            int b = (value >> (7 * i)) & 0x7F;
            packed |= b << (8 * i);
        }
        return packed;
    }

    private static int unpack(int packedValue, int byteCount) {
        int value = 0;
        for (int i = 0; i < byteCount; i++) {
            int b = (packedValue >> (8 * i)) & 0xFF;
            if (b > 0x7F) {
                throw new MidiError("Invalid packed MIDI byte 0x%02X at position %d"
                        .formatted(b, i));
            }
            value |= b << (7 * i);
        }
        return value;
    }

    @Override
    public String toString() {
        int digits = byteCount * 2;
        String format = "0x%0" + digits + "X (packed: 0x%0" + digits + "X, %d bytes)";
        return format.formatted(value, packedValue, byteCount);
    }
}
