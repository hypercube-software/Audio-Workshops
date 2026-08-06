package com.hypercube.workshop.midiworkshop.api.presets.standard.model;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record StandardPreset(StandardPresetId presetId, String name, List<DrumKitNote> drumMap) {
    private static final Pattern PATTERN = Pattern.compile("^(?<msb>\\d+)-(?<lsb>\\d+)-(?<prg>\\d+)(?:\\s+(?<name>.+))?$");

    public static StandardPreset of(String definition) {
        Matcher m = PATTERN.matcher(definition);
        if (m.matches()) {
            StandardPresetId presetId = StandardPresetId.of(m.group("msb") + "-" + m.group("lsb") + "-" + m.group("prg"));
            String name = Optional.ofNullable(m.group("name"))
                    .orElse("");
            return new StandardPreset(presetId, name, new ArrayList<>());
        }
        throw new MidiConfigError("Unexpected standard preset definition: " + definition);
    }

    public boolean matches(MidiPreset midiPreset) {
        return presetId.matches(midiPreset);
    }

}
