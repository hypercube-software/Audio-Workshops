package com.hypercube.workshop.midiworkshop.presets.korg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategoryType;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetDomain;
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
import com.hypercube.workshop.midiworkshop.presets.AbstractPredefinedPatchesTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This is not a test class, we use JUNIT to run quick experimentations...
 * Another experiment to see if we can extract patches names from the VST GUI using AutoIt
 */
@Slf4j
public class PredefinedKorgPatchesTest extends AbstractPredefinedPatchesTest {
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
                currentBank.setPresetFormat(device.getPresetFormat());
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
            MidiDevicePreset midiDevicePreset = new MidiDevicePreset(null, programName, command, category, null, List.of());

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
}
