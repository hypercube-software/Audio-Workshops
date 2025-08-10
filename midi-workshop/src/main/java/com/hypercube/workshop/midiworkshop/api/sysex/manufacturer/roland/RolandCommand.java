package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland.command.DataSetCommandParser;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland.command.RolandCommandParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum RolandCommand {
    DATASET1("Data Set 1", 0x12, new DataSetCommandParser());

    private static final Map<Integer, RolandCommand> commands = Arrays.stream(RolandCommand.values())
            .collect(Collectors.toMap(RolandCommand::getCode, Function.identity()));

    private final String name;
    private final int code;
    private final RolandCommandParser parser;

    public static RolandCommand get(int code) {
        return Optional.ofNullable(commands.get(code))
                .orElseThrow(() -> new MidiError("Unknown %s: 0x%02X".formatted(RolandCommand.class.getSimpleName(), code)));
    }
}
