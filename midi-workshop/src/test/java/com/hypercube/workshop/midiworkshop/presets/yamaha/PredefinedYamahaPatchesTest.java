package com.hypercube.workshop.midiworkshop.presets.yamaha;

import com.hypercube.workshop.midiworkshop.api.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetNaming;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.presets.AbstractPredefinedPatchesTest;
import com.hypercube.workshop.midiworkshop.presets.yamaha.parser.CS1XPresetsHTMLParser;
import com.hypercube.workshop.midiworkshop.presets.yamaha.parser.CS2XPresetsHTMLParser;
import com.hypercube.workshop.midiworkshop.presets.yamaha.parser.XGSpecParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This is not a test class, we use JUNIT to run quick experimentations...
 * Using PDF to TXT/HTML tools like tabula, it is possible to parse the result and generate what we want in our configuration for the midi library
 */
public class PredefinedYamahaPatchesTest extends AbstractPredefinedPatchesTest {
    @Test
    void generateCS1XGDomains() throws IOException {
        CS1XPresetsHTMLParser parser = new CS1XPresetsHTMLParser(new File("./src/test/resources/Yamaha/CS1x XG-voices.html"));
        parser.parse();
    }

    @Test
    void generateCS2XGDomains() throws IOException {
        CS2XPresetsHTMLParser cs1XPresetsCSVParser = new CS2XPresetsHTMLParser(new File("./src/test/resources/Yamaha/CS2x XG-voices.html"));
        cs1XPresetsCSVParser.parse();
    }


    @Test
    void generateXGPresets() throws IOException {

        MidiDeviceDefinition device = new MidiDeviceDefinition();
        device.setPresetNaming(MidiPresetNaming.YAMAHA_XG);
        device.setPresetFormat(MidiBankFormat.BANK_MSB_LSB_PRG);
        XGSpecParser xgSpecParser = new XGSpecParser(device);

        List<MidiPreset> midiPresets = xgSpecParser.parsePresets(new File("./src/test/resources/Yamaha/XG-voices.htm"));
        midiPresets.addAll(xgSpecParser.parseDrumKits(new File("./src/test/resources/Yamaha/XG-drums.htm")));
        List<String> patches = midiPresets
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
        saveText(patches, "xg/XGPatches.txt");
        var banks = xgSpecParser.parseBanks()
                .stream()
                .sorted(Comparator.comparing(MidiDeviceBank::getMSB)
                        .thenComparing(MidiDeviceBank::getLSB)
                        .thenComparing(MidiDeviceBank::getName))
                .map(b -> b.getCommand() + " " + b.getName())
                .toList();
        saveText(banks, "xg/XGBanks.txt");
    }
}
