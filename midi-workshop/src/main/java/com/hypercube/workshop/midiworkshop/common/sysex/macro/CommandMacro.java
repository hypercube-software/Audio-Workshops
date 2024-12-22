package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record CommandMacro(
        Path definitionFile,
        String name,
        List<String> parameters, String body) {
    public static final String COMMAND_LABEL_REGEXP = "[A-Z_ \\+\\-a-z0-9]+";
    private static Pattern commandDefinition = Pattern.compile("^(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)\\s*:\\s*(?<body>.+)".formatted(COMMAND_LABEL_REGEXP));

    public static CommandMacro parse(Path definitionFile, String definition) {
        var m = commandDefinition.matcher(definition);
        if (m.find()) {
            String name = m.group("name");
            String params = m.group("params");
            String body = m.group("body");
            List<String> paramArray = Optional.ofNullable(params)
                    .map(p -> Arrays.stream(p.split(","))
                            .map(String::trim)
                            .toList())
                    .orElse(List.of());
            return new CommandMacro(definitionFile, name, paramArray, body);
        } else {
            throw new MidiError("Invalid command definition: \"%s\" in file %s".formatted(definition, definitionFile.toString()));
        }
    }

    public String expand(CommandCall call) {
        if (call.parameters()
                .size() != parameters.size()) {
            throw new MidiError("argument count mismatch for macro %s. Provided %d, Expected %d".formatted(name, call.parameters()
                    .size(), parameters.size()));
        }
        String result = body;
        for (int idx = 0; idx < parameters.size(); idx++) {
            String paramName = parameters.get(idx);
            String paramValue = call.parameters()
                    .get(idx);
            result = result.replaceAll(paramName, expandValue(paramValue));
        }
        return result;
    }

    @Override
    public String toString() {
        String params = parameters.stream()
                .collect(Collectors.joining(","));
        String file = Optional.ofNullable(definitionFile)
                .map(p -> p.toString() + ": ")
                .orElse("");
        return "%s%s(%s) => %s".formatted(file, name, params, body);
    }

    private String expandValue(String paramValue) {
        if (paramValue.startsWith("0x")) {
            return "%02X".formatted(Integer.parseInt(paramValue.substring(2), 16));
        } else if (paramValue.startsWith("$")) {
            return "%02X".formatted(Integer.parseInt(paramValue.substring(1), 16));
        } else if (paramValue.startsWith("[") && paramValue.endsWith("]")) {
            return paramValue; // range like [12-45]
        } else {
            return "%02X".formatted(Integer.parseInt(paramValue, 10));
        }
    }

    public boolean match(String commandCall) {
        Pattern commandCallExp = Pattern.compile("(^|[\\s:])%s\\s*\\([^)]*\\)".formatted(nameToRegExp()));
        var m = commandCallExp.matcher(commandCall);
        var matches = m.find();
        return matches;
    }

    private String nameToRegExp() {
        return name.replace("+", "\\+")
                .replace("-", "\\-");
    }
}
