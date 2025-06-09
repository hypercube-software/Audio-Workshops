package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record MidiDevicePreset(String name, String command, String category, String filename) {
    public static MidiDevicePreset of(MidiBankFormat midiBankFormat, String definition) {
        final String command;
        final String category;
        final String name;
        final String filename;
        if (definition.startsWith("@")) {
            filename = definition.substring(1);
            List<String> parts = Arrays.stream(definition.split("\\[|\\]"))
                    .toList();
            if (parts.size() == 3) {
                String[] spec = parts.get(1)
                        .trim()
                        .split(",");
                category = spec[spec.length - 1];
                if (spec.length == 2) {
                    command = spec[0];
                } else {
                    command = null;
                }
                name = parts.get(2)
                        .trim();

            } else {
                category = null;
                command = null;
                name = definition.substring(1)
                        .trim();
            }
        } else {
            filename = null;
            List<String> parts = Arrays.stream(definition.split("\\|"))
                    .toList();
            command = parts.get(0)
                    .trim();
            category = parts.get(1)
                    .trim();
            name = parts.get(2)
                    .trim();
        }
        return new MidiDevicePreset(name, normalizeCommand(midiBankFormat, command), category, filename);
    }

    private static String normalizeCommand(MidiBankFormat midiBankFormat, String command) {
        if (command == null) {
            return null;
        }
        final String normalizedCommand;
        if (command.indexOf('-') == -1) {
            normalizedCommand = command;
        } else {
            normalizedCommand = Arrays.stream(command.split("-"))
                    .map(Integer::parseInt)
                    .map(v -> "%02X".formatted(v))
                    .collect(Collectors.joining());
        }
        return switch (midiBankFormat) {
            case NO_BANK_PRG -> pad(2, normalizedCommand);
            case BANK_MSB_PRG -> pad(4, normalizedCommand);
            case BANK_LSB_PRG -> pad(4, normalizedCommand);
            case BANK_MSB_LSB_PRG -> pad(6, normalizedCommand);
            case BANK_PRG_PRG -> pad(4, normalizedCommand);
        };
    }

    private static String pad(int size, String normalizedCommand) {
        String toPad = "0000000000" + normalizedCommand;
        return toPad.substring(toPad.length() - size);
    }
}
