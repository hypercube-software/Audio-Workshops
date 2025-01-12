package com.hypercube.workshop.midiworkshop.common.sysex.macro;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A macro is function generating multiple MIDI messages (typically SYSEX, but can be program changes or anything else)
 * <ul>
 *     <li>It operates at string level, not bytes, because it is way more convenient</li>
 *     <li>It is possible to call multiple macros inside a macro with ";"</li>
 *     <li>It is used like a function with parameters: A(), A(a,b) and parameters are injected in the generated payload</li>
 * </ul>
 * Note that:
 * <ul>
 *     <li>{@link CommandMacro} define the function in the form "name(p1,p2,p3) : return size: payload"</li>
 *     <li>{@link CommandCall} define a call to that function in the form "name(v1,v2,v3)"</li>
 * </ul>
 * For instance, here a command macro for a Yamaha TG-500:
 * <pre>Multi(channel) : 120 : F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 channel F7</pre>
 * <p>This macro use 1 parameter <b>channel</b>, the expected response size is <b>0x120</b> bytes and the payload is a SysEX
 * <p>Note: Response size is optional we can have things like this:</p>
 * <pre>Multi(channel) : --- : F0 43 20 7A 'LM  0065MU' 0000000000000000000000000000 00 channel F7</pre>
 */
public final class CommandMacro {
    public static final String COMMAND_NAME_REGEXP = "[A-Z_ \\+\\-a-z0-9]+";
    private static Pattern commandDefinition = Pattern.compile("^(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)\\s*:\\s*(?<body>.+)".formatted(COMMAND_NAME_REGEXP));
    private static Pattern decimalNumber = Pattern.compile("[0-9]+");
    private final File definitionFile;
    private final String name;
    private final List<String> parameters;
    private final String body;
    private final Pattern commandCallExp;

    public CommandMacro(
            File definitionFile,
            String name,
            List<String> parameters,
            String body) {
        this.definitionFile = definitionFile;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        // look for <macro name>(...) making sure we catch exactly the macro name because some macro can have the same prefix
        commandCallExp = Pattern.compile("(^|[\\s:])%s\\s*\\([^)]*\\)".formatted(nameToRegExp(name)));
    }

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
            throw new MidiConfigError("Invalid command definition: \"%s\" in file %s\nIt should be \"name() : size : payload\"".formatted(definition, definitionFile.toString()));
        }
    }

    /**
     * Given a CommandCall, inject the provided values into the body of this macro
     * <p>Note: you are supposed to call {@link #matches} before</p>
     *
     * @param call A call definition to this macro
     * @return resolved macro, with all parameters replaced by provided values
     */
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
            String replacement = " %s ".formatted(expandParameterValue(paramValue));
            replacement = replacement.replace("$", "\\$");
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

    /**
     * Parmaters values can express multiple bytes when they are in hexadecimal
     * <p>We allow "0x" and "$" syntax to express hexadecimal values</p>
     * <ul>
     *     <li>0x00FF will produce the string "00 FF"</li>
     *     <li>$00FF will produce the string "00 FF"</li>
     *     <li>0012 will produce the string "0C" because it is a decimal</li>
     *     <li>ranges like [4-15] will be returned "as is"</li>
     *     <li>strings like 'LM  8101VC' will be returned "as is"</li>
     *     <li>variable names like vv will be returned "as is"</li>
     * </ul>
     *
     * @param paramValue
     * @return
     */
    private String expandParameterValue(String paramValue) {
        if (paramValue.startsWith("0x")) {
            return paramValue.substring(2);
        } else if (paramValue.startsWith("$")) {
            return paramValue.substring(1);
        } else {
            // decimal numbers are converted to hexa
            var m = decimalNumber.matcher(paramValue);
            if (m.matches()) {
                return "%02X".formatted(Integer.parseInt(paramValue, 10));
            } else {
                // range like [12-45]
                // string like range like 'LM  8101VC'
                // variable name
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
    public boolean matches(String commandCall) {
        var m = commandCallExp.matcher(commandCall);
        var matches = m.find();
        return matches;
    }

    private String nameToRegExp(String name) {
        return name.replace("+", "\\+")
                .replace("-", "\\-");
    }

    public File definitionFile() {
        return definitionFile;
    }

    public String name() {
        return name;
    }

    public List<String> parameters() {
        return parameters;
    }

    public String body() {
        return body;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CommandMacro) obj;
        return Objects.equals(this.definitionFile, that.definitionFile) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.parameters, that.parameters) &&
                Objects.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitionFile, name, parameters, body);
    }

}
