package com.hypercube.workshop.midiworkshop.api.sysex.macro;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter()
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class CommandCall {
    private static final Pattern commandCall = Pattern.compile("(?<name>%s)\\s*\\(((?<params>[^)]+))*\\)".formatted(CommandMacro.COMMAND_NAME_REGEXP));
    private final String name;
    private final List<String> parameters;
    @Setter
    private CommandMacro macro;

    /**
     * Parse a list of command definitions: A();B();C()
     *
     * @param device      where to find the macro currently called
     * @param definitions the definitions to parse
     * @return parsed definition
     */
    public static List<CommandCall> parse(MidiDeviceDefinition device, String definitions) {
        return parse(CommandMacro.UNSAVED_MACRO, device, definitions);
    }

    /**
     * Parse a list of command definitions: A();B();C()
     *
     * @param configFile  where those definitions comes from (can be {@link CommandMacro#UNSAVED_MACRO}
     * @param device      where to find the macro currently called (use null to set the macro later)
     * @param definitions the definitions to parse
     * @return parsed definition
     */
    public static List<CommandCall> parse(File configFile, MidiDeviceDefinition device, String definitions) {
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
                        CommandCall cmd = new CommandCall(name, paramArray);
                        if (device != null) {
                            cmd.macro(device.getMacro(cmd));
                        }
                        return cmd;
                    } else {
                        throw new MidiError("Invalid command call definition: '%s' in file %s".formatted(definitions, configFile.getAbsolutePath()));
                    }
                })
                .toList();
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(name, String.join(",", parameters));
    }
}
