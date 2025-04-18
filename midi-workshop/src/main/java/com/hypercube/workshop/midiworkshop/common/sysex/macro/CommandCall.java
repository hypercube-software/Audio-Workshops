package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record CommandCall(String name, List<String> parameters) {
    private static Pattern commandCall = Pattern.compile("(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)".formatted(CommandMacro.COMMAND_NAME_REGEXP));

    /**
     * Parse a command definition
     *
     * @param configFile where this definition comes from
     * @param definition the definition to parse
     * @return parsed definition
     */
    public static CommandCall parse(File configFile, String definition) {
        var m = commandCall.matcher(definition.trim());
        if (m.find()) {
            String name = m.group("name")
                    .trim();
            String params = m.group("params");
            List<String> paramArray = Optional.ofNullable(params)
                    .map(p -> Arrays.stream(p.split(","))
                            .map(String::trim)
                            .toList())
                    .orElse(List.of());
            return new CommandCall(name, paramArray);
        } else {
            throw new MidiError("Invalid command call definition: '%s' in file %s".formatted(definition, configFile.getAbsolutePath()));
        }
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(name, parameters.stream()
                .collect(Collectors.joining(",")));
    }
}
