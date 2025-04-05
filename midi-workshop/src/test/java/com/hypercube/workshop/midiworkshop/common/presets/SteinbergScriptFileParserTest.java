package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceDefinition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

class SteinbergScriptFileParserTest {
    @Test
    void generateSoundCanvasPresets() throws IOException {

        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setMidiPresetNaming(MidiPresetNaming.SOUND_CANVAS);
        device.setPresetFormat(MidiBankFormat.BANK_MSB_LSB_PRG);
        device.setPresetNumbering(MidiPresetNumbering.FROM_ZERO);
        SteinbergScriptFileParser steinbergScriptFileParser = new SteinbergScriptFileParser(device, new File("./src/test/resources/steinberg-scripts/SC-88.txt"));
        List<String> lines = steinbergScriptFileParser.parse()
                .stream()
                .sorted(Comparator.comparing(MidiPreset::getBankMSB)
                        .thenComparing(MidiPreset::getBankLSB)
                        .thenComparing(MidiPreset::getProgram))
                .map(p -> "%d-%d-%d %s".formatted(p.getBankMSB(), p.getBankLSB(), p.getProgram(), p.getTitle()))
                .toList();
        Files.write(Path.of("./src/main/resources/sc/SoundCanvasPatches.txt"), lines, StandardOpenOption.CREATE);
    }
}