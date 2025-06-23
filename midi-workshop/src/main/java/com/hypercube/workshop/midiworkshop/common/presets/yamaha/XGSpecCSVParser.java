package com.hypercube.workshop.midiworkshop.common.presets.yamaha;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

@Slf4j
@RequiredArgsConstructor
public class XGSpecCSVParser {
    private final MidiDeviceDefinition device;
    private final File csvFile;
    private final MidiBankFormat midiBankFormat = MidiBankFormat.BANK_MSB_LSB_PRG;
    private final MidiPresetNumbering midiPresetNumbering = MidiPresetNumbering.FROM_ZERO;

    public List<MidiPreset> parse() {
        try {
            List<MidiPreset> presetList = new ArrayList<>();
            List<String> voicepageLines = new ArrayList<>();
            int count = 0;
            boolean inChart6 = false;
            for (String line : Files.readAllLines(csvFile.toPath())) {
                if (line.startsWith("[Attached Chart 6]")) {
                    count++;
                    if (count == 2) {
                        inChart6 = true;
                    }
                } else if (line.startsWith("[Attached Chart 7]")) {
                    inChart6 = false;
                }
                if (inChart6) {
                    if (line.startsWith("[Attached Chart")) {
                        if (voicepageLines.size() > 32) {
                            List<MidiPreset> pageResult = parsePage(voicepageLines);
                            for (MidiPreset midiPreset : pageResult) {
                                if (!presetList.contains(midiPreset)) {
                                    presetList.add(midiPreset);
                                }
                            }
                            voicepageLines = new ArrayList<>();
                        }
                    }
                    voicepageLines.add(line);
                }

            }
            return presetList;
        } catch (IOException e) {
            throw new MidiConfigError("Unable to read parse script file:" + csvFile.getAbsolutePath(), e);
        }
    }

    private List<MidiPreset> parsePage(List<String> pageLines) {
        String curCategory = null;
        // the entire table provide presets for 4 banks, except page 106 where there is 5 banks
        // bank 0 is always the default one (MSB/LSB 0), it is repeated on all pages
        String bankNames[] = new String[5];
        int bankMSB[] = new int[5];
        int bankLSB[] = new int[5];
        Matcher m = null;
        List<MidiPreset> result = new ArrayList<>();
        int currentPageLine = 0;
        for (String l : pageLines) {
            log.info("{}: {}", currentPageLine, l);
            String[] columns = l.split(",");
            if (l.startsWith(",Bank Select MSB")) {
                List<Integer> msbColumns = List.of(7, 15, 19, 23, 27);
                for (int i = 0; i < msbColumns.size(); i++) {
                    Integer idx = msbColumns.get(i);
                    if (idx >= columns.length) {
                        break;
                    }
                    String msb = columns[idx];
                    bankMSB[i] = msb.length() > 0 ? Integer.parseInt(msb) : -1;
                }
            }
            if (l.startsWith(",Bank Select LSB")) {
                List<Integer> lsbColumns = List.of(7, 15, 19, 23, 27);
                for (int i = 0; i < lsbColumns.size(); i++) {
                    Integer idx = lsbColumns.get(i);
                    if (idx >= columns.length) {
                        break;
                    }
                    String lsb = columns[idx];
                    bankLSB[i] = lsb.length() > 0 ? Integer.parseInt(lsb) : -1;
                }
            }
            if (currentPageLine == 18) {
                List<Integer> bankNamesColumns = List.of(7, 15, 19, 23, 27);
                for (int i = 0; i < bankNamesColumns.size(); i++) {
                    Integer idx = bankNamesColumns.get(i);
                    if (idx >= columns.length) {
                        break;
                    }
                    String bankName = columns[idx];
                    bankNames[i] = bankName.length() > 0 ? bankName : "default";
                }
            }
            if (columns.length >= 8) {
                String prg = columns[5];
                if (prg.length() == 0 || prg.equals("PGM#")) {
                    continue;
                }
                String category = columns[1];
                if (!category.isEmpty()) {
                    curCategory = category;
                }

                // programs are in the range [1-128] in the spec
                int program = Integer.parseInt(prg) - 1;

                List<Integer> patchNameColumn = List.of(7, 15, 19, 23, 27);
                for (int i = 0; i < patchNameColumn.size(); i++) {
                    Integer idx = patchNameColumn.get(i);
                    if (idx >= columns.length) {
                        break;
                    }
                    String name = columns[idx];
                    if (name.length() > 0) {
                        if (bankNames[0] == null) {
                            throw new RuntimeException("Bank names not founds");
                        }
                        int bankSelectMSB = bankMSB[i];
                        int bankSelectLSB = bankLSB[i];
                        String bankName = bankNames[i];
                        log.info("Bank: '{}' MSB/LSB: {}/{} prg: {} category: '{}' name: '{}'", bankName,
                                bankSelectMSB, bankSelectLSB, program, curCategory, name);
                        MidiPreset midiPreset = MidiPresetBuilder.parse(bankName, name, curCategory, device, 0, bankSelectMSB, bankSelectLSB, program);
                        if (midiPreset != null) {
                            result.add(midiPreset);
                        }
                    }
                }
            }
            currentPageLine++;
        }
        return result;
    }

    private Optional<MidiPreset> parsePreset(String command, String body) {
        String[] parts = command.split(",");
        if (parts.length == 4) {
            int program = Integer.parseInt(parts[1].trim(), 10);
            int bankSelectMSB = Integer.parseInt(parts[2].trim(), 10);
            int bankSelectLSB = Integer.parseInt(parts[3].trim(), 10);
            String presetName = body.trim();
            return Optional.of(MidiPresetBuilder.parse(presetName, device, 0, bankSelectMSB, bankSelectLSB, program));
        }
        return Optional.empty();
    }
}
