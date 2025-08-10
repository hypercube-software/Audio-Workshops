package com.hypercube.workshop.midiworkshop.api.sysex.macro;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record CommandCall(String name, List<String> parameters) {
    private static Pattern commandCall = Pattern.compile("(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)".formatted(CommandMacro.COMMAND_NAME_REGEXP));

    /**
     * Parse a list of command definitions: A();B();C()
     *
     * @param configFile  where this definition comes from
     * @param definitions the definitions to parse
     * @return parsed definition
     */
    public static List<CommandCall> parse(File configFile, String definitions) {
        return Arrays.stream(definitions.split(";"))
                .map(definition -> {
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
                        throw new MidiError("Invalid command call definition: '%s' in file %s".formatted(definitions, configFile.getAbsolutePath()));
                    }
                })
                .toList();
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(name, parameters.stream()
                .collect(Collectors.joining(",")));
    }
}
