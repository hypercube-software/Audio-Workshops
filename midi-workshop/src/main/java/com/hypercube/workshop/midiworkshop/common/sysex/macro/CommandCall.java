package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record CommandCall(String name, List<String> parameters) {
    private static Pattern commandCall = Pattern.compile("(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)".formatted(CommandMacro.COMMAND_LABEL_REGEXP));

    public static CommandCall parse(String definition) {
        var m = commandCall.matcher(definition);
        if (m.find()) {
            String name = m.group("name");
            String params = m.group("params");
            List<String> paramArray = Optional.ofNullable(params)
                    .map(p -> Arrays.stream(p.split(","))
                            .map(String::trim)
                            .toList())
                    .orElse(List.of());
            return new CommandCall(name, paramArray);
        } else {
            throw new MidiError("Invalid command definition: " + definition);
        }
    }
}
