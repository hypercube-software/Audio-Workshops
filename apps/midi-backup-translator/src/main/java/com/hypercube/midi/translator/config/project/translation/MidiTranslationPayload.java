package com.hypercube.midi.translator.config.project.translation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MidiTranslationPayload {
    private final int valueIndex;
    private final byte[] payload;
}
