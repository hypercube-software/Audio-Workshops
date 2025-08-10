package com.hypercube.workshop.midiworkshop.api.presets;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import lombok.Getter;

import java.io.File;
import java.util.regex.Pattern;

/**
 * This class represent a range of valid Program Change numbers for a specific device
 */
@Getter
public class MidiPresetRange {
    private static Pattern RANGE_REGEXP = Pattern.compile("^(\\d+)-(\\d+)");
    private static Pattern NORANGE_REGEXP = Pattern.compile("^(\\d+)");

    private final int from;
    private final int to;

    public MidiPresetRange(int program) {
        this.from = program;
        this.to = program;
    }

    public MidiPresetRange(int fromProgram, int toProgram) {
        this.from = fromProgram;
        this.to = toProgram;
    }

    public static MidiPresetRange parse(File definitionFile, String definition) {
        var m = RANGE_REGEXP.matcher(definition);
        if (m.matches()) {
            int from = Integer.parseInt(m.group(1), 10);
            int to = Integer.parseInt(m.group(2), 10);
            return new MidiPresetRange(from, to);
        } else {
            m = NORANGE_REGEXP.matcher(definition);
            if (m.matches()) {
                int program = Integer.parseInt(m.group(1), 10);
                return new MidiPresetRange(program);
            }
        }
        throw new MidiConfigError("Invalid preset domain definition: \"%s\" in file %s".formatted(definition, definitionFile.toString()));
    }
}
