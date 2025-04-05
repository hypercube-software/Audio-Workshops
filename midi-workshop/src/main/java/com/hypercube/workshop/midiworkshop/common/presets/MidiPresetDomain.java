package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class represent a valid bank with a list of valid program changes for a given device
 *
 * @param bank   bank number to be used to select the presets
 * @param ranges valid presets in this bank
 */
public record MidiPresetDomain(int bank, List<MidiPresetRange> ranges) {
    private static Pattern DOMAIN_REGEXP = Pattern.compile("^([0-9A-F]+)\\s*:\\s*(.+)");
    private static Pattern RANGE_REGEXP = Pattern.compile("([^,]+)");

    public static MidiPresetDomain parse(File definitionFile, MidiBankFormat presetFormat, String definition) {
        var m = DOMAIN_REGEXP.matcher(definition);
        if (m.matches()) {
            String bankNumber = m.group(1);
            String body = m.group(2);
            List<MidiPresetRange> ranges = new ArrayList<>();
            var m2 = RANGE_REGEXP.matcher(body);
            while (m2.find()) {
                String range = m2.group();
                ranges.add(MidiPresetRange.parse(definitionFile, range));
            }
            return new MidiPresetDomain(parseBankNumber(presetFormat, bankNumber), ranges);
        }
        throw new MidiConfigError("Invalid preset domain definition: \"%s\" in file %s".formatted(definition, definitionFile.toString()));
    }

    private static int parseBankNumber(MidiBankFormat presetFormat, String bankNumber) {
        if (bankNumber.startsWith("$")) {
            return Integer.parseInt(bankNumber.substring(1), 16);
        } else if (bankNumber.startsWith("0x")) {
            return Integer.parseInt(bankNumber.substring(2), 16);
        } else {
            return Integer.parseInt(bankNumber, 10);
        }
    }
}
