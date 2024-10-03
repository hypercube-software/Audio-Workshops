package com.hypercube.workshop.midiworkshop.common.presets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MidiPreset(MidiPresetFormat presetFormat, int bank, int program, String title) {
    @Override
    public String toString() {
        return "%d-%d".formatted(bank, program);
    }

    private static Pattern presetRegExp = Pattern.compile("(?<id1>[0-9]+)(-(?<id2>[0-9]+))?(\\s+(?<title>.*))?");

    public static MidiPreset of(MidiPresetFormat presetFormat, int bank, int program, String title) {
        return new MidiPreset(presetFormat, bank, program, title);
    }

    public static MidiPreset fromString(MidiPresetFormat presetFormat, String definition) {
        {
            Matcher matcher = presetRegExp.matcher(definition);
            if (matcher.find()) {
                try {
                    String id1 = matcher.group("id1");
                    String id2 = matcher.group("id2");
                    int program = id2 == null ? Integer.parseInt(id1) : Integer.parseInt(id2);
                    int bank = id2 == null ? 0 : Integer.parseInt(id1);
                    String title = matcher.group("title");
                    return MidiPreset.of(presetFormat, bank, program, title);
                } catch (NumberFormatException err) {
                    throw new IllegalArgumentException("Illegal preset name: " + definition);
                }
            } else {
                throw new IllegalArgumentException("Illegal preset name: " + definition);
            }
        }
    }
}
