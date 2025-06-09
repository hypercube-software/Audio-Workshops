package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class represent a valid list of program changes for a given command
 */
@Getter
@Setter
public class MidiPresetDomain {
    /**
     * valid presets in this bankName
     */
    private List<MidiPresetRange> ranges;

    private static Pattern RANGE_REGEXP = Pattern.compile("([^,;]+)");

    public static MidiPresetDomain parse(File definitionFile, MidiDeviceDefinition midiDeviceDefinition, String definition) {
        List<MidiPresetRange> ranges = new ArrayList<>();
        var m2 = RANGE_REGEXP.matcher(definition);
        while (m2.find()) {
            String range = m2.group();
            ranges.add(MidiPresetRange.parse(definitionFile, range));
        }
        if (ranges.size() > 0) {
            var domain = new MidiPresetDomain();
            domain.setRanges(ranges);
            return domain;
        }

        throw new MidiConfigError("Invalid preset domain definition: \"%s\" in file %s".formatted(definition, definitionFile.toString()));
    }
}
