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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class CS1XPresetsHTMLParser {
    private static final Pattern BANK_PATTERN = Pattern.compile("^Bank\\s+(\\d+)$");
    private static final Pattern PGM_PATTERN = Pattern.compile("^Pgm$", Pattern.CASE_INSENSITIVE);

    private final File htmlFile;

    public void parse() throws IOException {
        Document doc = Jsoup.parse(Files.readString(htmlFile.toPath()));
        Elements tables = doc.select("table");

        Map<Integer, TreeSet<Integer>> domains = new LinkedHashMap<>();
        TreeSet<Integer> sfxDomain = new TreeSet<>();

        for (Element table : tables) {
            parseTable(table, domains, sfxDomain);
        }

        writeYaml(domains, sfxDomain);
    }

    private void parseTable(Element table, Map<Integer, TreeSet<Integer>> domains, TreeSet<Integer> sfxDomain) {
        Elements trs = table.select("tr");
        List<List<String>> grid = new ArrayList<>();

        for (Element tr : trs) {
            Elements tds = tr.select("td");
            List<String> row = new ArrayList<>();
            for (Element td : tds) {
                row.add(td.text().trim());
            }
            grid.add(row);
        }

        if (grid.size() < 4) {
            return;
        }

        int bankHeaderRowIdx = -1;
        for (int i = 0; i < grid.size(); i++) {
            for (String cell : grid.get(i)) {
                if (BANK_PATTERN.matcher(cell).matches()) {
                    bankHeaderRowIdx = i;
                    break;
                }
            }
            if (bankHeaderRowIdx >= 0) {
                break;
            }
        }

        if (bankHeaderRowIdx < 0) {
            log.warn("No bank header row found in table, skipping");
            return;
        }

        List<String> bankHeaderRow = grid.get(bankHeaderRowIdx);
        Map<Integer, Integer> bankCols = new LinkedHashMap<>();
        for (int c = 0; c < bankHeaderRow.size(); c++) {
            Matcher m = BANK_PATTERN.matcher(bankHeaderRow.get(c));
            if (m.matches()) {
                bankCols.put(c, Integer.parseInt(m.group(1)));
            }
        }

        if (bankCols.isEmpty()) {
            log.warn("No bank columns found in table, skipping");
            return;
        }

        boolean isSfxTable = bankCols.size() == 1;

        int pgmCol = -1;
        for (int i = 0; i <= bankHeaderRowIdx; i++) {
            List<String> row = grid.get(i);
            for (int c = 0; c < row.size(); c++) {
                if (PGM_PATTERN.matcher(row.get(c)).matches()) {
                    pgmCol = c;
                    break;
                }
            }
            if (pgmCol >= 0) {
                break;
            }
        }

        if (pgmCol < 0) {
            log.warn("No Pgm column found in table, skipping");
            return;
        }

        int dataStart = bankHeaderRowIdx + 1;
        List<Integer> pgmNumbers = new ArrayList<>();
        for (int i = dataStart; i < grid.size(); i++) {
            List<String> row = grid.get(i);
            if (pgmCol >= row.size()) {
                continue;
            }
            String pgmStr = row.get(pgmCol);
            try {
                pgmNumbers.add(Integer.parseInt(pgmStr));
            } catch (NumberFormatException ignored) {
            }
        }

        if (!pgmNumbers.isEmpty()) {
            int expected = 1;
            for (int pgm : pgmNumbers) {
                if (pgm != expected) {
                    throw new IllegalStateException(
                            "HTML export corrupted: pgm numbers not consecutive. Expected " + expected + " but found " + pgm
                                    + ". Full sequence: " + pgmNumbers);
                }
                expected++;
            }
            if (expected - 1 != 128) {
                throw new IllegalStateException(
                        "HTML export corrupted: pgm numbers should go up to 128, but max is " + (expected - 1));
            }
        }

        for (int i = dataStart; i < grid.size(); i++) {
            List<String> row = grid.get(i);
            if (pgmCol >= row.size()) {
                continue;
            }
            String pgmStr = row.get(pgmCol);
            int pgm;
            try {
                pgm = Integer.parseInt(pgmStr);
            } catch (NumberFormatException e) {
                continue;
            }
            for (Map.Entry<Integer, Integer> e : bankCols.entrySet()) {
                int col = e.getKey();
                boolean present = col < row.size() && !row.get(col).isEmpty();
                if (present) {
                    int bankNum = e.getValue();
                    if (bankNum == 102) {
                        sfxDomain.add(pgm);
                    } else {
                        domains.computeIfAbsent(bankNum, k -> new TreeSet<>())
                                .add(pgm);
                    }
                }
            }
        }
    }

    private static String xgBankName(int bankNum) {
        String key = "0-%d".formatted(bankNum);
        String name = XGSpecParser.getBankNames().get(key);
        return name != null ? name : "XG %03d".formatted(bankNum);
    }

    private void writeYaml(Map<Integer, TreeSet<Integer>> domains, TreeSet<Integer> sfxDomain) throws IOException {
        Map<String, String> banks = new LinkedHashMap<>();
        for (int bank : domains.keySet()
                .stream()
                .sorted()
                .toList()) {
            String name = xgBankName(bank);
            String command = "$%04X".formatted(bank);
            String domain = consolidateZeroBased(domains.get(bank));
            banks.put(name, "%s %s".formatted(command, domain));
        }
        String yaml = buildYaml(banks, "$4000", consolidateZeroBased(sfxDomain));
        log.info("\n" + yaml);
        Path outputPath = Path.of("./target/patches/xg/CS1XDomains.yml");
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
