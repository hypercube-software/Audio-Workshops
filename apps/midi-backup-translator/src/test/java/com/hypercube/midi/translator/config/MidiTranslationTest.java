package com.hypercube.midi.translator.config;

import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.config.project.translation.MidiTranslationPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MidiTranslationTest {

    @Test
    void scaleCC() {
        MidiTranslation midiTranslation = new MidiTranslation(127, -6, 10, List.of(new MidiTranslationPayload(0, new byte[]{0})));
        assertEquals(midiTranslation.getLowerBound(), midiTranslation.scaledCC(0));
        assertEquals(midiTranslation.getUpperBound(), midiTranslation.scaledCC(127));
        assertEquals(2, midiTranslation.scaledCC(64));
    }

}