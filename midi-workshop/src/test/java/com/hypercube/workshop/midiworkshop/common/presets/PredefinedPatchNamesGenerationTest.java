package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.presets.steinberg.SteinbergScriptFileParser;
import com.hypercube.workshop.midiworkshop.common.presets.yamaha.CS1XPresetsCSVParser;
import com.hypercube.workshop.midiworkshop.common.presets.yamaha.XGSpecParser;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class PredefinedPatchNamesGenerationTest {
    @Test
    void generateSoundCanvasPresets() throws IOException {

        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setPresetNaming(MidiPresetNaming.SOUND_CANVAS);
        device.setPresetFormat(MidiBankFormat.BANK_MSB_LSB_PRG);
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
        XGSpecParser xgSpecParser = new XGSpecParser(device);

        List<MidiPreset> midiPresets = xgSpecParser.parsePresets(new File("./src/test/resources/XG/XG-voices.htm"));
        midiPresets.addAll(xgSpecParser.parseDrumKits(new File("./src/test/resources/XG/XG-drums.htm")));
        List<String> lines = midiPresets
                .stream()
                .sorted(Comparator.comparing(MidiPreset::getBankMSB)
                        .thenComparing(MidiPreset::getBankLSB)
                        .thenComparing(MidiPreset::getLastProgram))
                .flatMap(p -> {
                            String command = "%d-%d-%d %s".formatted(p.getBankMSB(), p.getBankLSB(), p.getLastProgram(), p.getId()
                                    .name());
                            List<String> result = new ArrayList<>();
                            result.add(command);
                            result.addAll(p.getDrumKitNotes()
                                    .stream()
                                    .map(n -> "    %d %s".formatted(n.note(), n.title()))
                                    .toList());
                            return result.stream();
                        }
                )
                .toList();
        File dest = new File("./src/main/resources/xg/XGPatches.txt");
        dest.delete();
        Files.write(dest.toPath(), lines, StandardOpenOption.CREATE);
        lines = xgSpecParser.parseBanks()
                .stream()
                .sorted(Comparator.comparing(MidiDeviceBank::getMSB)
                        .thenComparing(MidiDeviceBank::getLSB)
                        .thenComparing(MidiDeviceBank::getName))
                .map(b -> b.getCommand() + " " + b.getName())
                .toList();
        dest = new File("./src/main/resources/xg/XGBanks.txt");
        dest.delete();
        Files.write(dest.toPath(), lines, StandardOpenOption.CREATE);
    }
}