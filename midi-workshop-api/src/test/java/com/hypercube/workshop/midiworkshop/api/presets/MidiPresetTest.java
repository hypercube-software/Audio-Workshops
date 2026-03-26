package com.hypercube.workshop.midiworkshop.api.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MidiPresetTest {
    private static Stream<TestParam> parseMSBLSBBank() {
        return Stream.of(
                new TestParam(MidiBankFormat.NO_BANK_PRG, "10", 0, 16),
                new TestParam(MidiBankFormat.BANK_MSB_PRG, "8-10", 0x0008, 10),
                new TestParam(MidiBankFormat.BANK_MSB_PRG, "080A", 0x0008, 10),
                new TestParam(MidiBankFormat.BANK_LSB_PRG, "8-108", 0x0008, 108),
                new TestParam(MidiBankFormat.BANK_LSB_PRG, "086C", 0x0008, 108),
                new TestParam(MidiBankFormat.BANK_MSB_LSB_PRG, "2-8-108", 0x0208, 108),
                new TestParam(MidiBankFormat.BANK_MSB_LSB_PRG, "02086C", 0x0208, 108)
        );
    }

    private static Stream<TestParam> parseBrokenMSBLSBBank() {
        return Stream.of(
                new TestParam(MidiBankFormat.NO_BANK_PRG, "4-2", 0, 0),
                new TestParam(MidiBankFormat.NO_BANK_PRG, "8", 0, 0),
                new TestParam(MidiBankFormat.BANK_MSB_PRG, "8", 0, 0),
                new TestParam(MidiBankFormat.BANK_LSB_PRG, "8", 0, 0),
                new TestParam(MidiBankFormat.BANK_MSB_LSB_PRG, "2-8", 0, 0)
        );
    }

    private MidiDeviceDefinition forgeDevice(TestParam testParam) {
        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setDefinitionFile(new File("config.yml"));
        device.setPresetFormat(testParam.midiBankFormat);
        return device;
    }

    record TestParam(MidiBankFormat midiBankFormat, String input, int expectedBank,
                     int expectedProgram) {
    }

    @ParameterizedTest
    @MethodSource
    void parseMSBLSBBank(TestParam testParam) {
        MidiPreset preset = MidiPresetBuilder.parse(forgeDevice(testParam), 1, "title", List.of(), List.of(testParam.input()), List.of(), List.of());
        assertEquals("title", preset.getId()
                .name());
        assertEquals(1, preset.getZeroBasedChannel());
        assertEquals(testParam.expectedBank(), preset.getBank());
        assertEquals(testParam.expectedProgram(), preset.getLastProgram());
    }

    @ParameterizedTest
    @MethodSource
    void parseBrokenMSBLSBBank(TestParam testParam) {
        assertThrows(MidiConfigError.class, () ->
                MidiPresetBuilder.parse(forgeDevice(testParam), 1, "title", List.of(), List.of(testParam.input()), List.of(), List.of()));
    }
}
