package com.hypercube.workshop.midiworkshop.common.presets;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record MidiPresetCategory(String name, List<String> aliases) {

    public static MidiPresetCategory of(String definition) {
        String[] parts = definition.split(":");
        String name = parts[0].trim();
        List<String> aliases = parts.length == 2 ? Arrays.stream(parts[1].split(","))
                .map(String::trim)
                .toList() : List.of();
        return new MidiPresetCategory(name, Stream.of(List.of(name), aliases)
                .flatMap(Collection::stream)
                .toList());
    }

    private boolean allAliasesHaveSize2() {
        return aliases.size() > 0 && aliases.stream()
                .filter(a -> a.length() != 2)
                .findFirst()
                .isEmpty();
    }

    public boolean matches(String presetName) {
        if (allAliasesHaveSize2()) {
            // Yamaha category codes
            return aliases.stream()
                    .filter(a -> presetName.startsWith(a))
                    .findFirst()
                    .isPresent();
        } else {
            return aliases.stream()
                    .filter(a -> presetName.toLowerCase()
                            .contains(a.toLowerCase()))
                    .findFirst()
                    .isPresent();
        }
    }
}
