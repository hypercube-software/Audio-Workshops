package com.hypercube.workshop.midiworkshop.api.presets;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode
public class MidiPresetCategory {
    public static String UNKNOWN = "Unknown";
    private String name;
    private MidiPresetCategoryType type;
    private List<String> aliases;

    public static MidiPresetCategory of(String definition) {
        String[] parts = definition.split(":");
        String name = parts[0].trim();
        List<String> aliases = parts.length == 2 ? Arrays.stream(parts[1].split(","))
                .map(String::trim)
                .toList() : List.of();
        MidiPresetCategoryType type = getType(aliases);
        // we include also the name as an alias
        aliases = Stream.of(List.of(name), aliases)
                .flatMap(Collection::stream)
                .toList();
        return new MidiPresetCategory(name, type, aliases);
    }

    /**
     * If first alias is only with 2 Letters, it is a Yamaha category
     */
    private static MidiPresetCategoryType getType(List<String> aliases) {
        return !aliases.isEmpty()
                && aliases.getFirst()
                .equals(aliases.getFirst()
                        .toUpperCase())
                && aliases.getFirst()
                .length() == 2
                ? MidiPresetCategoryType.YAMAHA : MidiPresetCategoryType.REGULAR;
    }

    public boolean matches(String presetName) {
        return switch (type) {
            case YAMAHA -> matchesYamaha(presetName);
            case REGULAR -> matchesRegular(presetName);
        };
    }

    @Override
    public String toString() {
        return name;
    }

    private boolean matchesRegular(String presetName) {
        return aliases.stream()
                .anyMatch(a -> presetName.toLowerCase()
                        .contains(a.toLowerCase()));
    }

    private boolean matchesYamaha(String presetName) {
        return aliases.stream()
                .anyMatch(a -> (a.length() == 2 && presetName.startsWith(a)) || (a.length() > 2 && presetName.toLowerCase()
                        .contains(a.toLowerCase())));
    }
}
