package com.hypercube.midi.translator.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MidiTranslation {
    private final int cc;
    private final int valueIndex;
    private final byte[] payload;
    private final int lowerBound;
    private final int upperBound;

    public int scaledCC(int value) {
        int range = upperBound - lowerBound + 1;
        int scaledCC = range * value / 128;
        return scaledCC + lowerBound;
    }

}
