package com.hypercube.workshop.midiworkshop.presets.yamaha;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class CS2XPresetsHTMLParser {
    private static final Pattern BANK_PATTERN = Pattern.compile("^Bank\\s+(\\d+)$");
    private final File htmlFile;

    public void parse() throws IOException {
        Document doc = Jsoup.parse(Files.readString(htmlFile.toPath()));
        Elements tables = doc.select("table[border=1]");
        log.info("Scanning {} tables...", tables.size());
        Map<Integer, TreeSet<Integer>> domains = new HashMap<>();
        TreeSet<Integer> sfxDomain = new TreeSet<>();
        for (Element table : tables) {
            parseTable(table, domains, sfxDomain);
        }
        writeYaml(domains, sfxDomain);
    }

    private void parseTable(Element table, Map<Integer, TreeSet<Integer>> domains, TreeSet<Integer> sfxDomain) {
        List<List<String>> grid = expandTable(table);
        int bankHeaderRowIdx = -1;
        for (int i = 0; i < grid.size(); i++) {
            List<String> row = grid.get(i);
            if (!row.isEmpty() && row.get(0)
                    .trim()
                    .equals("Bank Select LSB")) {
                bankHeaderRowIdx = i;
                break;
            }
        }
        if (bankHeaderRowIdx == -1) {
            return;
        }
        // Detect the "Bank N" columns from the raw header cells, tracking each
        // bank's horizontal span. In the SFX table the single "Bank 0" header
        // spans two columns via colspan, unlike the normal voice tables where
        // each "Bank N" occupies exactly one column.
        Map<Integer, int[]> bankSpans = new LinkedHashMap<>();
        int cellStart = 0;
        Element headerTr = table.select("tr")
                .get(bankHeaderRowIdx);
        for (Element td : headerTr.select("td")) {
            int colspan = td.hasAttr("colspan") ? Integer.parseInt(td.attr("colspan")) : 1;
            Matcher m = BANK_PATTERN.matcher(td.text()
                    .trim());
            if (m.matches()) {
                bankSpans.put(Integer.parseInt(m.group(1)), new int[] { cellStart, cellStart + colspan - 1 });
            }
            cellStart += colspan;
        }
        if (bankSpans.isEmpty()) {
            return;
        }
        // The SFX table is the only one holding a single "Bank 0" (spanned by
// colspan), distinct from the normal voice tables which list several banks.
boolean isSfxTable = bankSpans.size() == 1 && bankSpans.containsKey(0);
        int pgmHeaderRowIdx = -1;
        int pgmCol = -1;
        for (int i = bankHeaderRowIdx + 1; i < grid.size() && pgmCol == -1; i++) {
            List<String> row = grid.get(i);
            for (int c = 0; c < row.size(); c++) {
                if (row.get(c)
                        .trim()
                        .toLowerCase()
                        .startsWith("pgm#")) {
                    pgmHeaderRowIdx = i;
                    pgmCol = c;
                    break;
                }
            }
        }
        if (pgmCol == -1) {
            return;
        }
        for (int i = pgmHeaderRowIdx + 1; i < grid.size(); i++) {
            List<String> row = grid.get(i);
            if (pgmCol >= row.size()) {
                continue;
            }
            String pgmStr = row.get(pgmCol)
                    .trim();
            int pgm;
            try {
                pgm = Integer.parseInt(pgmStr);
            } catch (NumberFormatException e) {
                continue;
            }
            for (Map.Entry<Integer, int[]> e : bankSpans.entrySet()) {
                int bank = e.getKey();
                int[] span = e.getValue();
                // The value column is the last cell of the bank's span: for the
                // SFX table the header spans the name and the value columns, so
                // only the trailing column holds the bank-data marker.
                int col = span[1];
                boolean present = col < row.size() && !row.get(col)
                        .trim()
                        .isEmpty();
                if (present) {
                    if (isSfxTable) {
                        sfxDomain.add(pgm);
                    } else {
                        domains.computeIfAbsent(bank, k -> new TreeSet<>())
                                .add(pgm);
                    }
                }
            }
        }
    }

    private void writeYaml(Map<Integer, TreeSet<Integer>> domains, TreeSet<Integer> sfxDomain) throws IOException {
        Map<String, String> banks = new LinkedHashMap<>();
        for (int bank : domains.keySet()
                .stream()
                .sorted()
                .toList()) {
            String name = "XG %03d".formatted(bank);
            String command = "$%04X".formatted(bank);
            String domain = consolidateZeroBased(domains.get(bank));
            banks.put(name, "%s %s".formatted(command, domain));
        }
        String yaml = buildYaml(banks, "$4000", consolidateZeroBased(sfxDomain));
        log.info("\n{}", yaml);
        Path outputPath = Path.of("./target/patches/xg/CS2XDomains.yml");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, yaml.getBytes());
    }

    private String buildYaml(Map<String, String> banks, String sfxCommand, String sfxDomain) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : banks.entrySet()) {
            String[] parts = e.getValue()
                    .split(" ", 2);
            sb.append("\"%s\":\n".formatted(e.getKey()));
            sb.append("  command: \"%s\"\n".formatted(parts[0]));
            sb.append("  presetDomain: %s\n".formatted(parts[1]));
        }
        sb.append("\"XG SFX\":\n");
        sb.append("  command: \"%s\"\n".formatted(sfxCommand));
        sb.append("  presetDomain: %s\n".formatted(sfxDomain));
        return sb.toString();
    }

    private List<List<String>> expandTable(Element table) {
        List<List<String>> grid = new ArrayList<>();
        Map<Integer, Integer> pendingRows = new HashMap<>();
        Map<Integer, String> pendingValue = new HashMap<>();
        for (Element tr : table.select("tr")) {
            List<String> row = new ArrayList<>();
            int col = 0;
            for (Element td : tr.select("td")) {
                while (pendingRows.containsKey(col)) {
                    row.add(pendingValue.get(col));
                    int remaining = pendingRows.get(col) - 1;
                    if (remaining == 0) {
                        pendingRows.remove(col);
                        pendingValue.remove(col);
                    } else {
                        pendingRows.put(col, remaining);
                    }
                    col++;
                }
                String value = td.text()
                        .trim();
                int colspan = 1;
                if (td.hasAttr("colspan")) {
                    colspan = Integer.parseInt(td.attr("colspan"));
                }
                int rowspan = 1;
                if (td.hasAttr("rowspan")) {
                    rowspan = Integer.parseInt(td.attr("rowspan"));
                }
                for (int c = 0; c < colspan; c++) {
                    row.add(value);
                    if (rowspan > 1) {
                        pendingRows.put(col, rowspan - 1);
                        pendingValue.put(col, value);
                    }
                    col++;
                }
            }
            while (pendingRows.containsKey(col)) {
                row.add(pendingValue.get(col));
                int remaining = pendingRows.get(col) - 1;
                if (remaining == 0) {
                    pendingRows.remove(col);
                    pendingValue.remove(col);
                } else {
                    pendingRows.put(col, remaining);
                }
                col++;
            }
            grid.add(row);
        }
        return grid;
    }

    private String consolidateZeroBased(Collection<Integer> oneBasedValues) {
        if (oneBasedValues.isEmpty()) {
            return "";
        }
        return consolidateRanges(oneBasedValues.stream()
                .map(v -> v - 1)
                .sorted()
                .toList());
    }

    private String consolidateRanges(List<Integer> sorted) {
        List<String> parts = new ArrayList<>();
        int start = sorted.get(0);
        int prev = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            int v = sorted.get(i);
            if (v == prev + 1) {
                prev = v;
            } else {
                parts.add(formatRange(start, prev));
                start = v;
                prev = v;
            }
        }
        parts.add(formatRange(start, prev));
        return String.join(",", parts);
    }

    private String formatRange(int start, int end) {
        return start == end ? String.valueOf(start) : "%d-%d".formatted(start, end);
    }
}
