package com.hypercube.workshop.midiworkshop.common.presets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MidiPresetTest {
    record TestParam(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String input, int expectedBank,
                     int expectedProgram) {
    }

    private static Stream<TestParam> parseMSBLSBBank() {
        return Stream.of(
                new TestParam(MidiBankFormat.NO_BANK, MidiPresetNumbering.FROM_ZERO, "10", 0, 10),
                new TestParam(MidiBankFormat.BANK_MSB_PRG, MidiPresetNumbering.FROM_ZERO, "10", 0, 10),
                new TestParam(MidiBankFormat.BANK_MSB_LSB_PRG, MidiPresetNumbering.FROM_ZERO, "2-8-108", 2 << 7 | 8, 108));
    }

    @ParameterizedTest
    @MethodSource
    void parseMSBLSBBank(TestParam testParam) {
        MidiPreset preset = MidiPreset.of(new File("config.yml"), 1, testParam.midiBankFormat(), testParam.presetNumbering(), "title", List.of(), List.of(testParam.input()), List.of(), List.of());
        assertEquals("title", preset.title());
        assertEquals(1, preset.channel());
    }

}