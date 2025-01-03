package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record CommandMacro(
        File definitionFile,
        String name,
        List<String> parameters, String body) {
    public static final String COMMAND_NAME_REGEXP = "[A-Z_ \\+\\-a-z0-9]+";
    private static Pattern commandDefinition = Pattern.compile("^(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)\\s*:\\s*(?<body>.+)".formatted(COMMAND_NAME_REGEXP));
    private static Pattern decimalNumber = Pattern.compile("[0-9]+");

    /**
     * Parse the definition of a macro, found in YAML conf files
     *
     * @param definitionFile YAML file
     * @param definition     macro definition, something like "name(params) : body"
     * @return
     */
    public static CommandMacro parse(File definitionFile, String definition) {
        var m = commandDefinition.matcher(definition);
        if (m.find()) {
            String name = m.group("name");
            String params = m.group("params");
            String body = m.group("body"); // body is " size : payload"
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
            String paramName = " %s ".formatted(parameters.get(idx));
            String paramValue = call.parameters()
                    .get(idx);
            String replacement = " %s ".formatted(expandValue(paramValue));
            result = result.replaceAll(paramName, replacement);
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
            return paramValue.substring(2);
        } else if (paramValue.startsWith("$")) {
            return paramValue.substring(1);
        } else {
            var m = decimalNumber.matcher(paramValue);
            if (m.matches()) {
                return "%02X".formatted(Integer.parseInt(paramValue, 10));
            } else {
                // range like [12-45]
                // string like range like 'LM  8101VC'
                // varaible name
                return paramValue;
            }
        }
    }

    /**
     * Tell if a string contains a command call to this macro
     *
     * @param commandCall
     * @return true if found
     */
    public boolean match(String commandCall) {
        // look for <macro name>(...) making sure we catch exactly the macro name because some macro can have the same prefix
        String pattern = "(^|[\\s:])%s\\s*\\([^)]*\\)".formatted(nameToRegExp());
        Pattern commandCallExp = Pattern.compile(pattern);
        var m = commandCallExp.matcher(commandCall);
        var matches = m.find();
        return matches;
    }

    private String nameToRegExp() {
        return name.replace("+", "\\+")
                .replace("-", "\\-");
    }
}
