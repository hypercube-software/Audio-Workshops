package com.hypercube.workshop.midiworkshop.common.presets;

import java.util.Arrays;
import java.util.List;

public record MidiPresetCategory(String name, List<String> aliases) {

    public static MidiPresetCategory of(String definition) {
        String[] parts = definition.split(":");
        String name = parts[0].trim();
        List<String> aliases = parts.length == 2 ? Arrays.stream(parts[1].split(","))
                .map(String::trim)
                .toList() : List.of();
        return new MidiPresetCategory(name, aliases);
    }

    public boolean matches(String presetName) {
        return aliases.stream()
                .filter(a -> presetName.startsWith(a + " "))
                .findFirst()
                .isPresent();
    }
}
