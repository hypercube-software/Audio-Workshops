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
        // For regular aliases, we include also the name as an alias
        aliases = switch (type) {
            case YAMAHA -> aliases;
            case REGULAR -> Stream.of(List.of(name), aliases)
                    .flatMap(Collection::stream)
                    .toList();
        };
        return new MidiPresetCategory(name, type, aliases);
    }

    /**
     * If all aliases are only with 2 Letters, there are Yamaha categories
     */
    private static MidiPresetCategoryType getType(List<String> aliases) {
        return aliases.size() > 0 && aliases.stream()
                .filter(a -> a.length() != 2)
                .findFirst()
                .isEmpty() ? MidiPresetCategoryType.YAMAHA : MidiPresetCategoryType.REGULAR;
    }

    public boolean matches(String presetName) {
        return switch (type) {
            case YAMAHA -> matchesYamaha(presetName);
            case REGULAR -> matchesRegular(presetName);
        };
    }

    private boolean matchesRegular(String presetName) {
        return aliases.stream()
                .filter(a -> presetName.toLowerCase()
                        .contains(a.toLowerCase()))
                .findFirst()
                .isPresent();
    }

    private boolean matchesYamaha(String presetName) {
        return aliases.stream()
                .filter(a -> presetName.startsWith(a))
                .findFirst()
                .isPresent();
    }

    @Override
    public String toString() {
        return name;
    }
}
