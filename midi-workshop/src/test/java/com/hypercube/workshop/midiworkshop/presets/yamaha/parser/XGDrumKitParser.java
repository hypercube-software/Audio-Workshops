package com.hypercube.workshop.midiworkshop.presets.yamaha.parser;

import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class XGDrumKitParser {
    private final MidiDeviceDefinition device;

    private static List<String> getAllCells(Element row) {
        return row.select("td")
                .stream()
                .map(Element::text)
                .collect(Collectors.toList());
    }

    public List<MidiPreset> parse(File htmlFile) {
        Document doc;
        try {
            doc = Jsoup.parse(Files.readString(htmlFile.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Elements tables = doc.select("table");
        log.info("Scanning {} tables...", tables.size());

        List<MidiPreset> allPresets = new ArrayList<>();
        for (Element table : tables) {
            allPresets.addAll(parseTable(table));
        }
        return allPresets.stream()
                .collect(Collectors.toMap(
                        p -> p.getBankMSB() + "-" + p.getBankLSB() + "-" + p.getBankPrg(),
                        p -> p,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .toList();
    }

    private List<MidiPreset> parseTable(Element table) {
        Elements rows = table.select("tr");
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> firstRowCells = getAllCells(rows.get(0));
        if (!firstRowCells.get(0).trim().equals("Bank Select MSB")) {
            return List.of();
        }

        List<String> msbRow = firstRowCells;
        List<String> lsbRow = getAllCells(rows.get(1));
        List<String> pgmRow = getAllCells(rows.get(2));
        List<String> nameRow = getAllCells(rows.get(4));

        List<Integer> valuePositions = new ArrayList<>();
        for (int i = 1; i < msbRow.size(); i++) {
            String cell = msbRow.get(i).trim();
            if (cell.equals("127") || cell.equals("126")) {
                valuePositions.add(i);
            }
        }

        if (valuePositions.isEmpty()) {
            return List.of();
        }

        List<Integer> msbValues = valuePositions.stream()
                .map(i -> Integer.parseInt(msbRow.get(i).trim()))
                .toList();
        List<Integer> lsbValues = valuePositions.stream()
                .map(i -> Integer.parseInt(lsbRow.get(i).trim()))
                .toList();
        List<Integer> prgValues = valuePositions.stream()
                .map(i -> Integer.parseInt(pgmRow.get(i).trim()) - 1)
                .toList();

        List<Integer> namePositions = new ArrayList<>();
        List<String> kitNames = new ArrayList<>();
        for (int i = 1; i < nameRow.size(); i++) {
            String cell = nameRow.get(i).trim();
            if (!cell.isEmpty() && !cell.equals("O")) {
                namePositions.add(i);
                kitNames.add(cell);
            }
        }

        log.info("  Table with {} kits: {}", kitNames.size(), kitNames);

        int noteDataStart = findNoteDataStart(rows);

        List<Map<Integer, String>> kitNotes = new ArrayList<>();
        for (int k = 0; k < kitNames.size(); k++) {
            kitNotes.add(new LinkedHashMap<>());
        }

        for (int r = noteDataStart; r < rows.size(); r++) {
            List<String> cells = getAllCells(rows.get(r));
            String firstCell = cells.get(0).trim();
            if (firstCell.isEmpty()) {
                continue;
            }
            int midiNote = Integer.parseInt(firstCell.split(" ")[0]) - 1;

            for (int k = 0; k < kitNames.size(); k++) {
                int pos = namePositions.get(k);
                if (pos < cells.size()) {
                    String instrument = cells.get(pos).trim();
                    if (!instrument.isEmpty()) {
                        kitNotes.get(k).put(midiNote, instrument);
                    }
                }
            }
        }

        Map<Integer, String> baseNotes = kitNotes.get(0);
        boolean isDrumKit = msbValues.get(0) == 127;
        String bankName = isDrumKit ? "XG DRUM KITS" : "XG SFX KITS";

        List<MidiPreset> presets = new ArrayList<>();
        for (int k = 0; k < kitNames.size(); k++) {
            String kitName = kitNames.get(k);
            int msb = msbValues.get(k);
            int lsb = lsbValues.get(k);
            int prg = prgValues.get(k);

            MidiPreset preset = MidiPresetBuilder.parse(bankName, kitName, "DrumKit", device, 0,
                    msb, lsb, prg);

            Map<Integer, String> mergedNotes = isDrumKit
                    ? new LinkedHashMap<>(baseNotes)
                    : new LinkedHashMap<>();
            mergedNotes.putAll(kitNotes.get(k));

            mergedNotes.forEach((note, title) ->
                    preset.getDrumKitNotes().add(new DrumKitNote(title, note)));

            presets.add(preset);
        }

        log.info("  Generated {} presets", presets.size());
        return presets;
    }

    private int findNoteDataStart(Elements rows) {
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            List<String> cells = getAllCells(row);
            if (!cells.isEmpty() && cells.get(0).trim().startsWith("Note#")) {
                return i + 1;
            }
        }
        throw new RuntimeException("Note# header not found");
    }
}
