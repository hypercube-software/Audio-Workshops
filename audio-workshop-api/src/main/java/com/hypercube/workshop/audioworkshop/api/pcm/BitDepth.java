package com.hypercube.workshop.audioworkshop.api.pcm;

public enum BitDepth {
    BIT_DEPTH_8,
    BIT_DEPTH_16,
    BIT_DEPTH_24,
    BIT_DEPTH_32;

    public static BitDepth valueOf(int sampleSizeInBits) {
        return switch (sampleSizeInBits) {
            case 8 -> BIT_DEPTH_8;
            case 16 -> BIT_DEPTH_16;
            case 24 -> BIT_DEPTH_24;
            case 32 -> BIT_DEPTH_32;
            default -> throw new IllegalArgumentException("Bitdepth not supported:" + sampleSizeInBits);
        };
    }

    public int getBytes() {
        return getBits() / 8;
    }

    public int getBits() {
        return switch (this) {
            case BIT_DEPTH_8 -> 8;
            case BIT_DEPTH_16 -> 16;
            case BIT_DEPTH_24 -> 24;
            case BIT_DEPTH_32 -> 32;
        };
    }
}
