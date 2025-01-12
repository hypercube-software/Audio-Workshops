package com.hypercube.midi.translator.config.project.translation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class MidiTranslation {
    private final int cc;
    private final int lowerBound;
    private final int upperBound;
    private final List<MidiTranslationPayload> payloads;

    public int scaledCC(int value) {
        int range = upperBound - lowerBound + 1;
        int scaledCC = range * value / 128;
        return scaledCC + lowerBound;
    }

}
