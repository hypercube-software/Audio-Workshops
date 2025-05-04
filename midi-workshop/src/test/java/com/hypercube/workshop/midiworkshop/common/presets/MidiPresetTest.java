package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MidiPresetTest {
    record TestParam(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering, String input, int expectedBank,
                     int expectedProgram) {
    }

    private static Stream<TestParam> parseMSBLSBBank() {
        return Stream.of(
                new TestParam(MidiBankFormat.NO_BANK_PRG, MidiPresetNumbering.FROM_ZERO, "10", 0, 10),
                new TestParam(MidiBankFormat.BANK_MSB_PRG, MidiPresetNumbering.FROM_ZERO, "8-10", 0x0008, 10),
                new TestParam(MidiBankFormat.BANK_LSB_PRG, MidiPresetNumbering.FROM_ZERO, "8-108", 0x0008, 108),
                new TestParam(MidiBankFormat.BANK_MSB_LSB_PRG, MidiPresetNumbering.FROM_ZERO, "2-8-108", 0x0208, 108)
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseMSBLSBBank(TestParam testParam) {
        MidiPreset preset = MidiPresetBuilder.parse(new File("config.yml"), 1, testParam.midiBankFormat(), testParam.presetNumbering(), "title", List.of(), List.of(testParam.input()), List.of(), List.of());
        assertEquals("title", preset.getId()
                .name());
        assertEquals(1, preset.getZeroBasedChannel());
        assertEquals(testParam.expectedProgram(), preset.getLastProgram());
        assertEquals(testParam.expectedBank(), preset.getBank());
    }

    private static Stream<TestParam> parseBrokenMSBLSBBank() {
        return Stream.of(
                new TestParam(MidiBankFormat.NO_BANK_PRG, MidiPresetNumbering.FROM_ZERO, "4-2", 0, 0),
                new TestParam(MidiBankFormat.BANK_MSB_PRG, MidiPresetNumbering.FROM_ZERO, "8", 0, 0),
                new TestParam(MidiBankFormat.BANK_LSB_PRG, MidiPresetNumbering.FROM_ZERO, "8", 0, 0),
                new TestParam(MidiBankFormat.BANK_MSB_LSB_PRG, MidiPresetNumbering.FROM_ZERO, "2-8", 0, 0)
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseBrokenMSBLSBBank(TestParam testParam) {
        assertThrows(MidiConfigError.class, () ->
                MidiPresetBuilder.parse(new File("config.yml"), 1, testParam.midiBankFormat(), testParam.presetNumbering(), "title", List.of(), List.of(testParam.input()), List.of(), List.of()));
    }
}