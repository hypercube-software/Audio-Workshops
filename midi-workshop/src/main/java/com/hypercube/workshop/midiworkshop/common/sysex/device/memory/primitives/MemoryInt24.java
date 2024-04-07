package com.hypercube.workshop.midiworkshop.common.sysex.device.memory.primitives;

/**
 * This class implement the conversion of 7bits packed integers which are used in SYSEX
 * <ul>
 * <li>All MIDI specs use packed integers to represent memory addresses and sizes
 * <li>They can't be used to do arythmetics, so we need to convert them to regular integers
 * </ul>
 */
public record MemoryInt24(int value, int packedValue) {

    public static MemoryInt24 from(String value, boolean packed) {
        if (value == null)
            return null;
        int address = Integer.parseInt(value, 16);
        return packed ? MemoryInt24.fromPacked(address) : MemoryInt24.from(address);
    }

    public static MemoryInt24 from(int unpackedValue) {
        return new MemoryInt24(unpackedValue, packInt24(unpackedValue));
    }

    public static MemoryInt24 fromPacked(int packedValue) {
        return new MemoryInt24(unPackInt24(packedValue), packedValue);
    }

    public static int unPackInt24(int value) {
        int b0 = value & 0xFF;
        int b1 = (value >> 8) & 0xFF;
        int b2 = (value >> 16) & 0xFF;
        return b0 | (b1 << 7) | (b2 << 14);
    }

    public static int packInt24(int value) {
        int b0 = value & 0x7F;
        int b1 = (value >> 7) & 0x7F;
        int b2 = (value >> 14) & 0x7F;
        return b0 | (b1 << 8) | (b2 << 16);
    }

    public MemoryInt24 add(int amount) {
        return MemoryInt24.from(value + amount);
    }

    public MemoryInt24 add(MemoryInt24 amount) {
        return MemoryInt24.from(value + amount.value);
    }

    public MemoryInt24 div(int amount) {
        return MemoryInt24.from(value / amount);
    }

    public MemoryInt24 mul(int amount) {
        return MemoryInt24.from(value * amount);
    }

    @Override
    public String toString() {
        return "0x%06X (packed: 0x%06X)".formatted(value, packedValue);
    }

    public int[] getPackedBytes() {
        int[] data = new int[3];
        data[0] = packedValue >> 16 & 0xFF;
        data[1] = packedValue >> 8 & 0xFF;
        data[2] = packedValue & 0xFF;
        return data;
    }
}
