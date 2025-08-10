package com.hypercube.workshop.midiworkshop.api.presets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.presets.steinberg.SteinbergScriptFileParser;
import com.hypercube.workshop.midiworkshop.api.presets.yamaha.CS1XPresetsCSVParser;
import com.hypercube.workshop.midiworkshop.api.presets.yamaha.XGSpecParser;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceBankMixinWithCommand;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceDefinitionMixinWithCategories;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceModeMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer.MidiDevicePresetSerializer;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer.MidiPresetCategorySerializer;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer.MidiPresetDomainSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
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
    void generateKorgM1Banks() throws IOException {
        List<String> content = Files.readAllLines(Path.of("./src/test/resources/Korg/Korg M1 VST Programs.txt"))
                .stream()
                .map(String::trim)
                .toList();
        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setDeviceName("M1 VST");
        device.setBrand("Korg");
        MidiDeviceMode mode = new MidiDeviceMode();
        mode.setName("Program");
        List<String> categories = new ArrayList<>();
        categories.add("Piano");
        categories.add("Keyboard");
        categories.add("Organ");
        categories.add("Bell/Mallet");
        categories.add("Strings");
        categories.add("Woodwinds");
        categories.add("Brass");
        categories.add("Vocal");
        categories.add("Guitar");
        categories.add("Bass");
        categories.add("Synth Lead");
        categories.add("Synth Poly");
        categories.add("Synth Pad");
        categories.add("Synth Motion");
        categories.add("SFX / Complex");
        categories.add("Drums / Perc");
        device.setCategories(categories.stream()
                .map(c -> new MidiPresetCategory(c, MidiPresetCategoryType.REGULAR, List.of()))
                .toList());
        device.getDeviceModes()
                .put(mode.getName(), mode);
        MidiDeviceBank currentBank = null;
        for (String l : content) {
            // fix OCR errors
            l = l.replace("@", "0")
                    .replace("é", "6")
                    .replace("Pos G", "P S G 1");
            String[] values = l.split("\\|");
            if (values.length != 5) {
                log.info("ERROR: " + l);
                continue;
            }
            int card = Integer.parseInt(values[0]);
            String cardName = values[1];
            cardName = cardName.substring(cardName.indexOf("/") + 1)
                    .replace("/", " ")
                    .trim();
            if (cardName.endsWith(".")) {
                cardName = cardName.substring(0, cardName.length() - 1);
            }
            cardName = "%02d %s".formatted(card + 1, cardName);
            currentBank = mode.getBanks()
                    .get(cardName);
            if (currentBank == null) {
                currentBank = new MidiDeviceBank();
                currentBank.setName(cardName);
                currentBank.setCommand("$00%02X".formatted(card));
                currentBank.setPresets(new ArrayList<>());
                mode.getBanks()
                        .put(cardName, currentBank);
            }
            int cc = Integer.parseInt(values[2]);
            int catId = Integer.parseInt(values[3]);
            String programName = values[4];
            String command = "00%02X%02X".formatted(card, cc);
            String category = device.getCategories()
                    .get(catId)
                    .name();
            MidiDevicePreset midiDevicePreset = new MidiDevicePreset(programName, command, category, null, List.of());

            currentBank.getPresets()
                    .add(midiDevicePreset);
        }
        mode.getBanks()
                .values()
                .forEach(b -> b.setPresetDomain(MidiPresetDomain.parse(device.getDefinitionFile(), device, "%d-%d".formatted(0, b.getPresets()
                        .size()))));

        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.addMixIn(MidiDeviceBank.class, MidiDeviceBankMixinWithCommand.class);
        mapper.addMixIn(MidiDeviceDefinition.class, MidiDeviceDefinitionMixinWithCategories.class);
        mapper.addMixIn(MidiDeviceMode.class, MidiDeviceModeMixin.class);
        SimpleModule module = new SimpleModule();
        module.addSerializer(MidiDevicePreset.class, new MidiDevicePresetSerializer());
        module.addSerializer(MidiPresetDomain.class, new MidiPresetDomainSerializer());
        module.addSerializer(MidiPresetCategory.class, new MidiPresetCategorySerializer());
        mapper.registerModule(module);
        mapper.writeValue(new File("M1 VST.yml"), device);
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