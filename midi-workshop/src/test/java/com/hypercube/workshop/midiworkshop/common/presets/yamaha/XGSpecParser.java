package com.hypercube.workshop.midiworkshop.common.presets.yamaha;

import com.hypercube.workshop.midiworkshop.common.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class XGSpecParser {
    private final MidiDeviceDefinition device;
    private final MidiBankFormat midiBankFormat = MidiBankFormat.BANK_MSB_LSB_PRG;
    private Map<String, String> bankNames = buildBankNames();
    private Map<String, String> drumKitsNames = buildKitNames();
    private static final Pattern SINGLE_LETTER_PATTERN = Pattern.compile("^[0-9+-]$");
    private static final Pattern NOTE_PATTERN = Pattern.compile("^[CDEFGAB#]{1,2}(\\s[0-9-]+)?$");

    public List<MidiDeviceBank> parseBanks() {
        return bankNames.keySet()
                .stream()
                .map(k -> {
                    String name = bankNames.get(k);
                    MidiDeviceBank b = new MidiDeviceBank();
                    b.setName(name);
                    b.setCommand(k);
                    return b;
                })
                .toList();
    }

    public List<MidiPreset> parsePresets(File htmlFile) {
        Document doc = null;
        try {
            doc = Jsoup.parse(Files.readString(htmlFile.toPath()));
            Elements tables = doc.select("body>ul>li>table table[border=1]");
            Elements tables2 = doc.select("body>ul>li>table[border=1]");
            tables.addAll(tables2);
            log.info("Scanning " + tables.size() + " tables...");
            return tables.stream()
                    .flatMap(t -> parseVoiceTable(t).stream())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MidiPreset> parseDrumKits(File htmlFile) {
        Document doc = null;
        try {
            doc = Jsoup.parse(Files.readString(htmlFile.toPath()));
            Elements tables = doc.select("body>ul>li>table[border=1]");
            log.info("Scanning " + tables.size() + " tables...");
            var result = tables.stream()
                    .flatMap(t -> parseDrumTable(t).stream())
                    .distinct()
                    .toList();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MidiPreset> parseDrumTable(Element table) {
        Map<String, MidiPreset> presetMap = new HashMap<>();
        Elements rows = table.select("tr");
        log.info(rows.size() + " rows");
        int start = 0;
        List<String> msbRow = getAllCells(rows.get(start));
        List<String> lsbRow = getAllCells(rows.get(start + 1));
        List<String> prgRow = getAllCells(rows.get(start + 2));
        String msb = msbRow.get(0);
        String lsb = lsbRow.get(0);
        String prg = prgRow.get(0);
        log.info("{} {} {}", msb, lsb, prg);
        if (!(prg.equals("PGM# (1-128)")) || !msb.equals("Bank Select MSB") || !lsb.equals("Bank Select LSB") || msbRow.size() != lsbRow.size() && msbRow.size() != prgRow.size()) {
            throw new RuntimeException();
        }
        List<Integer> msbList = IntStream.range(1, msbRow.size())
                .mapToObj(i -> msbRow.get(i))
                .map(Integer::parseInt)
                .toList();
        List<Integer> lsbList = IntStream.range(1, lsbRow.size())
                .mapToObj(i -> lsbRow.get(i))
                .map(Integer::parseInt)
                .toList();
        List<Integer> prgList = IntStream.range(1, prgRow.size())
                .mapToObj(i -> prgRow.get(i))
                .map(v -> Integer.parseInt(v) - 1)
                .toList();
        start = -1;
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            List<String> cells = getAllCells(row);
            if (cells.get(0)
                    .equals("Note#")) {
                start = i;
                break;
            }
        }
        if (start == -1) {
            throw new RuntimeException();
        }
        start++;
        for (int i = start; i < rows.size(); i++) {
            Element row = rows.get(i);
            List<String> cells = getAllCells(row);
            int note = -1;
            int bank = 0;
            for (int c = 0; c < cells.size(); c++) {
                String value = cells.get(c)
                        .trim();
                String nextValue1 = c + 1 < cells.size() ? cells.get(c + 1) : ""
                        .trim();
                String nextValue2 = c + 2 < cells.size() ? cells.get(c + 2) : ""
                        .trim();
                if (c == 0) {
                    note = Integer.parseInt(value) - 1;
                }
                if (value.length() > 3 && (SINGLE_LETTER_PATTERN.matcher(nextValue1)
                        .matches() || SINGLE_LETTER_PATTERN.matcher(nextValue2)
                        .matches()) && !NOTE_PATTERN.matcher(value)
                        .matches()) {
                    String noteName = value;
                    if (!noteName.isEmpty()) {
                        int bankSelectMSB = msbList.get(bank);
                        int bankSelectLSB = lsbList.get(bank);
                        int program = prgList.get(bank);
                        String command = "%d-%d-%d".formatted(bankSelectMSB, bankSelectLSB, program);
                        String name = drumKitsNames.get(command);
                        String bankName = bankNames.get("%d-%d".formatted(bankSelectMSB, bankSelectLSB));
                        log.info("Bank: {} Msb: {} Lsb: {} Prg: {} '{}' - {} {}", bankName,
                                bankSelectMSB,
                                bankSelectLSB,
                                program,
                                name, note, noteName);
                        if (name == null) {
                            throw new RuntimeException("Undeclared kit");
                        }
                        MidiPreset midiPreset = presetMap.get(command);
                        if (midiPreset == null) {
                            midiPreset = MidiPresetBuilder.parse(bankName, name, "DrumKit", device, 0,
                                    bankSelectMSB, bankSelectLSB, program);
                            if (midiPreset == null) {
                                throw new RuntimeException("Unable to forge MidiPreset");
                            }
                            presetMap.put(command, midiPreset);
                        }
                        midiPreset.getDrumKitNotes()
                                .add(new DrumKitNote(noteName, note));
                    }
                    bank++;
                }
            }
        }
        return presetMap.values()
                .stream()
                .toList();
    }

    private List<MidiPreset> parseVoiceTable(Element table) {
        List<MidiPreset> result = new ArrayList<>();
        Elements rows = table.select("tr");
        log.info(rows.size() + " rows");
        int start = rows.size() == 67 ? 0 : 1;
        List<String> msbRow = getAllCells(rows.get(start));
        List<String> lsbRow = getAllCells(rows.get(start + 1));
        List<String> columnNames = getAllCells(rows.get(start + 2));
        String msb = msbRow.get(0);
        String lsb = lsbRow.get(0);
        String grp = columnNames.get(0);
        if (!(grp.equals("Instrument Group") || grp.equals("PGM#")) || !msb.equals("Bank Select MSB") || !lsb.equals("Bank Select LSB") || msbRow.size() != lsbRow.size()) {
            throw new RuntimeException();
        }
        List<Integer> msbList = IntStream.range(1, msbRow.size())
                .mapToObj(i -> msbRow.get(i))
                .map(Integer::parseInt)
                .toList();
        List<Integer> lsbList = IntStream.range(1, lsbRow.size())
                .mapToObj(i -> lsbRow.get(i))
                .map(Integer::parseInt)
                .toList();
        log.info("MSB: " + msbList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        log.info("LSB: " + lsbList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        for (int i = start + 3; i < rows.size(); i++) {
            Element row = rows.get(i);
            List<String> cells = getAllCells(row);
            int missingCells = columnNames.size() - cells.size();
            for (int c = 0; c < missingCells; c++) {
                cells.add("");
            }
            int program = -1;
            int bank = 0;
            String category = "FX";
            for (int c = 0; c < columnNames.size(); c++) {
                String colName = columnNames.get(c)
                        .trim();
                String value = cells.get(c)
                        .trim();
                if (colName.equals("PGM#")) {
                    program = Integer.parseInt(value) - 1;
                } else if (colName.equals("Instrument Group")) {
                    category = value;
                } else if (colName.equals("Name")) {
                    if (!value.isEmpty()) {
                        String name = value;
                        Integer bankSelectMSB = msbList.get(bank);
                        Integer bankSelectLSB = lsbList.get(bank);
                        String bankName = getXGBankName(bankSelectMSB, bankSelectLSB);
                        log.info("    Bank: {} MSB: {} LSB: {} PRG:{} NAME:{}", bank, bankSelectMSB, bankSelectLSB, program, name);
                        if (bankName == null) {
                            throw new RuntimeException("Undeclared XG bank");
                        }
                        MidiPreset midiPreset = MidiPresetBuilder.parse(bankName, name, category, device, 0, bankSelectMSB, bankSelectLSB, program);
                        if (midiPreset != null) {
                            result.add(midiPreset);
                        }
                    }
                    bank++;
                }
            }
        }
        return result;
    }

    public String getXGBankName(int bankSelectMSB, int bankSelectLSB) {
        String key = "%d-%d".formatted(bankSelectMSB, bankSelectLSB);
        return bankNames.get(key);
    }

    private Map<String, String> buildKitNames() {
        Map<String, String> map = new HashMap<>();
        map.put("126-0-16", "Techno Kit K/S");
        map.put("126-0-17", "Techno Kit Hi");
        map.put("126-0-0", "SFX Kit 1");
        map.put("126-0-18", "Techno Kit Lo");
        map.put("126-0-1", "SFX Kit 2");
        map.put("126-0-32", "Sakura Kit");
        map.put("126-0-33", "Small Latin Kit");
        map.put("126-0-34", "China Kit");
        map.put("126-0-40", "Live! AfroCuban Kit");
        map.put("126-0-41", "Live! AfroCuban Kit 2");
        map.put("126-0-42", "Live! Brazilian Kit");
        map.put("126-0-43", "Live! PopLatin Kit");
        map.put("127-0-8", "Room Kit");
        map.put("127-0-9", "Dark Room Kit");
        map.put("127-0-16", "Rock Kit");
        map.put("127-0-17", "Rock Kit 2");
        map.put("127-0-0", "Standard Kit");
        map.put("127-0-24", "Electro Kit");
        map.put("127-0-25", "Analog Kit");
        map.put("127-0-26", "Analog Kit 2");
        map.put("127-0-27", "Dance Kit");
        map.put("127-0-1", "Standard Kit 2");
        map.put("127-0-28", "Hip Hop Kit");
        map.put("127-0-29", "Jungle Kit");
        map.put("127-0-30", "Apogee Kit");
        map.put("127-0-31", "Perigee Kit");
        map.put("127-0-32", "Jazz Kit");
        map.put("127-0-33", "Jazz Kit 2");
        map.put("127-0-2", "Dry Kit");
        map.put("127-0-40", "Brush Kit");
        map.put("127-0-41", "Brush Kit 2");
        map.put("127-0-3", "Bright Kit");
        map.put("127-0-48", "Symphony Kit");
        map.put("127-0-56", "Natural Kit");
        map.put("127-0-57", "Natural Funk Kit");
        map.put("127-0-4", "Slim Kit");
        map.put("127-0-64", "Tramp Kit");
        map.put("127-0-65", "Amber Kit");
        map.put("127-0-66", "Coffin Kit");
        map.put("127-0-5", "Cl™ Vil Slim Kit");
        map.put("127-0-6", "Rogue Kit");
        map.put("127-0-80", "Live! Standard Kit");
        map.put("127-0-81", "Live! Funk Kit");
        map.put("127-0-82", "Live! Brush Kit");
        map.put("127-0-83", "Live! Standard + Percussion Kit");
        map.put("127-0-84", "Live! Funk + Percussion Kit");
        map.put("127-0-85", "Live! Brush + Percussion Kit");
        map.put("127-0-7", "Hob Kit");

        return map;
    }

    private Map<String, String> buildBankNames() {
        Map<String, String> map = new HashMap<>();
        map.put("0-0", "XG 000");
        map.put("0-1", "XG 001 (KSP)");
        map.put("0-3", "XG 003 (Stereo)");
        map.put("0-6", "XG 006 (Single)");
        map.put("0-8", "XG 008 (Slow)");
        map.put("0-12", "XG 012 (Fast Decay)");
        map.put("0-14", "XG 014 (Double Attack)");
        map.put("0-16", "XG 016 (Bright 1)");
        map.put("0-17", "XG 017 (Bright 2)");
        map.put("0-18", "XG 018 (Dark 1)");
        map.put("0-19", "XG 019 (Dark 2)");
        map.put("0-20", "XG 020 (Resonant)");
        map.put("0-21", "XG 021 (LFO Cutoff Freq)");
        map.put("0-22", "XG 021 (Velo Cutoff Freq)");
        map.put("0-24", "XG 024 (Attack)");
        map.put("0-25", "XG 025 (Release)");
        map.put("0-26", "XG 026 (Sweep)");
        map.put("0-27", "XG 027 (Resonant Sweep)");
        map.put("0-28", "XG 028 (Muted)");
        map.put("0-29", "XG 029 (Complex FEG)");
        map.put("0-32", "XG 032 (Detune 1)");
        map.put("0-33", "XG 033 (Detune 2)");
        map.put("0-34", "XG 034 (Detune 3)");
        map.put("0-35", "XG 035 (Octave 1)");
        map.put("0-36", "XG 036 (Octave 2)");
        map.put("0-37", "XG 037 (5th 1)");
        map.put("0-38", "XG 038 (5th 2)");
        map.put("0-39", "XG 039 (Bend)");
        map.put("0-40", "XG 040 (Tutti 1)");
        map.put("0-41", "XG 041 (Tutti 2)");
        map.put("0-42", "XG 042 (Tutti 3)");
        map.put("0-43", "XG 043 (Velo-Switch)");
        map.put("0-45", "XG 045 (Velo-Xfade)");
        map.put("0-48", "XG 048 (Detune 4)");
        map.put("0-52", "XG 052 (Tutti 4)");
        map.put("0-53", "XG 053 (Tutti 5)");
        map.put("0-54", "XG 054 (Tutti 6)");
        map.put("0-64", "XG 064 (Other waves 1)");
        map.put("0-65", "XG 065 (Other waves 2)");
        map.put("0-66", "XG 066 (Other waves 3)");
        map.put("0-67", "XG 067 (Other waves 4)");
        map.put("0-68", "XG 068 (Other waves 5)");
        map.put("0-69", "XG 069 (Other waves 6)");
        map.put("0-70", "XG 070 (Other waves 7)");
        map.put("0-71", "XG 071 (Other waves 8)");
        map.put("0-72", "XG 072 (Other waves 9)");
        map.put("0-73", "XG 073 (Other waves 10)");
        map.put("0-74", "XG 074 (Other waves 11)");
        map.put("0-75", "XG 075 (Other waves 12)");
        map.put("0-76", "XG 076 (Other waves 13)");
        map.put("0-77", "XG 077 (Other waves 14)");
        map.put("0-78", "XG 078 (Other waves 15)");
        map.put("0-79", "XG 079 (Other waves 16)");
        map.put("0-80", "XG 080 (Other waves 17)");
        map.put("0-81", "XG 081 (Other waves 18)");
        map.put("0-82", "XG 082 (Other waves 19)");
        map.put("0-83", "XG 083 (Other waves 20)");
        map.put("0-84", "XG 084 (Other waves 21)");
        map.put("0-85", "XG 085 (Other waves 22)");
        map.put("0-86", "XG 086 (Other waves 23)");
        map.put("0-87", "XG 087 (Other waves 24)");
        map.put("0-88", "XG 088 (Other waves 25)");
        map.put("0-89", "XG 089 (Other waves 26)");
        map.put("0-90", "XG 090 (Other waves 27)");
        map.put("0-91", "XG 091 (Other waves 28)");
        map.put("0-92", "XG 092 (Other waves 29)");
        map.put("0-93", "XG 093 (Other waves 30)");
        map.put("0-94", "XG 094 (Other waves 31)");
        map.put("0-96", "XG 096 (Other instr 1)");
        map.put("0-97", "XG 097 (Other instr 2)");
        map.put("0-98", "XG 098 (Other instr 3)");
        map.put("0-99", "XG 099 (Other instr 4)");
        map.put("0-100", "XG 100 (Other instr 5)");
        map.put("0-101", "XG 101 (Other instr 6)");
        map.put("64-0", "XG SFX");
        map.put("126-0", "XG SFX KITS");
        map.put("127-0", "XG DRUM KITS");
        return map;
    }

    private static List<String> getAllCells(Element row) {
        return row
                .select("td")
                .stream()
                .map(Element::text)
                .collect(Collectors.toList());
    }
}
