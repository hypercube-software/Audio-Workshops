package com.hypercube.workshop.midiworkshop.api.presets.yamaha;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CS1XPresetsCSVParser {
    private final File csvFile;

    public void parse() throws IOException {
        Map<Integer, Set<Integer>> domains = new HashMap<>();
        Map<Integer, String> colNames = new HashMap<>();
        String content = Files.readString(csvFile.toPath());
        String current = "";
        boolean inStr = false;
        List<List<String>> records = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        for (int c : content.chars()
                .toArray()) {
            if (!inStr && c == '\"') {
                inStr = true;
            } else if (inStr && c == '\"') {
                inStr = false;
            } else if (!inStr && c == ',') {
                cells.add(current);
                current = "";
            } else if (!inStr && c == 10) {
                cells.add(current);
                current = "";
                log.info("====> " + cells.stream()
                        .collect(Collectors.joining(",")));
                records.add(cells);
                cells = new ArrayList<>();
            } else if (c == 13) {
                current += " ";
            } else if (c != 10) {
                current += (char) c;
            }

            if (current.contains("Bank 102")) {
                log.info("Last table Bank 102");
            }
        }
        for (List<String> record : records) {
            String firstRow = record.get(0)
                    .trim();
            if (firstRow
                    .startsWith("Instrument Group") || firstRow.startsWith("Pgm")) {
                log.info(record.stream()
                        .collect(Collectors.joining(",")));
                for (int i = 0; i < record.size(); i++) {
                    colNames.put(i, record.get(i));
                }
            } else if (colNames.size() != 0) {
                int prg = -1;
                int bank = -1;
                String name = null;
                for (int i = 0; i < record.size(); i++) {
                    String colName = colNames.get(i);
                    String value = record.get(i)
                            .trim();
                    if (!value.isEmpty()) {
                        if (colName.equals("Pgm #") || colName.equals("Pg#m")) {
                            prg = Integer.parseInt(value) - 1;
                        } else if (colName.startsWith("Bank ")) {
                            bank = Integer.parseInt(colName.substring(5));
                            name = value;
                        }
                        if (prg != -1 && bank != -1) {
                            log.info("Bank {}: Prog: {} Preset: {}", bank, prg, name);
                            if (!domains.containsKey(bank)) {
                                domains.put(bank, new HashSet<>());
                            }
                            domains.get(bank)
                                    .add(prg);
                        }
                    }
                }

            }
        }
        for (int bank : domains.keySet()
                .stream()
                .sorted()
                .toList()) {
            List<Integer> values = domains.get(bank)
                    .stream()
                    .sorted()
                    .toList();
            String domain = "";
            int start = -1;
            int end = -1;
            int prev = -1;
            for (int i = 0; i < values.size(); i++) {
                int prg = values.get(i);
                if (start == -1) {
                    start = prg;
                } else if (prev == prg - 1) {
                    end = i;
                } else if (prev != prg - 1) {
                    end = prev;
                    domain += start != end ? ",%d-%d".formatted(start, end) : ",%d".formatted(start);
                    start = prg;
                    end = -1;
                }
                prev = prg;
                end = prg;
            }
            domain += start != end ? ",%d-%d".formatted(start, end) : ",%d".formatted(start);
            /*log.info(values.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));*/
            domain = domain.substring(1);
            log.info("Domain for bank " + bank + " : " + domain);
        }
    }
}
