package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.presets.steinberg.SteinbergScriptFileParser;
import com.hypercube.workshop.midiworkshop.common.presets.yamaha.CS1XPresetsCSVParser;
import com.hypercube.workshop.midiworkshop.common.presets.yamaha.XGSpecCSVParser;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

class PredefinedPatchNamesGenerationTest {
    @Test
    void generateSoundCanvasPresets() throws IOException {

        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setPresetNaming(MidiPresetNaming.SOUND_CANVAS);
        device.setPresetFormat(MidiBankFormat.BANK_MSB_LSB_PRG);
        device.setPresetNumbering(MidiPresetNumbering.FROM_ZERO);
        SteinbergScriptFileParser steinbergScriptFileParser = new SteinbergScriptFileParser(device, new File("./src/test/resources/steinberg-scripts/SC-88.txt"));
        List<String> lines = steinbergScriptFileParser.parse()
                .stream()
                .sorted(Comparator.comparing(MidiPreset::getBankMSB)
                        .thenComparing(MidiPreset::getBankLSB)
                        .thenComparing(MidiPreset::getLastProgram))
                .map(p -> "%d-%d-%d %s".formatted(p.getBankMSB(), p.getBankLSB(), p.getLastProgram(), p.getId()
                        .name()))
                .toList();
        Files.write(Path.of("./src/main/resources/sc/SoundCanvasPatches.txt"), lines, StandardOpenOption.CREATE);
    }

    @Test
    void generateCS1XGDomains() throws IOException {
        CS1XPresetsCSVParser cs1XPresetsCSVParser = new CS1XPresetsCSVParser(new File("./src/test/resources/XG/CS1xE2.csv"));
        cs1XPresetsCSVParser.parse();
    }

    @Test
    void generateXGPresets() throws IOException {

        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setPresetNaming(MidiPresetNaming.YAMAHA_XG);
        device.setPresetFormat(MidiBankFormat.BANK_MSB_LSB_PRG);
        device.setPresetNumbering(MidiPresetNumbering.FROM_ZERO);
        XGSpecCSVParser xgSpecCSVParser = new XGSpecCSVParser(device, new File("./src/test/resources/XG/XGspec2-00e.csv"));
        List<String> lines = xgSpecCSVParser.parse()
                .stream()
                .sorted(Comparator.comparing(MidiPreset::getBankMSB)
                        .thenComparing(MidiPreset::getBankLSB)
                        .thenComparing(MidiPreset::getLastProgram))
                .map(p -> "%d-%d-%d %s".formatted(p.getBankMSB(), p.getBankLSB(), p.getLastProgram(), p.getId()
                        .name()))
                .toList();
        Files.write(Path.of("./src/main/resources/xg/XGPatches.txt"), lines, StandardOpenOption.CREATE);
    }
}