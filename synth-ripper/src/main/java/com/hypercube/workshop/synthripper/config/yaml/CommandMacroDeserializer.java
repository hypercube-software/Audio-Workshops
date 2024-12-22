package com.hypercube.workshop.synthripper.config.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;

import java.io.IOException;
import java.nio.file.Path;

public class CommandMacroDeserializer extends StdDeserializer<CommandMacro> {
    private final Path macroFile;

    public CommandMacroDeserializer(Path macroFile) {
        super((Class<?>) null);
        this.macroFile = macroFile;
    }

    @Override
    public CommandMacro deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return CommandMacro.parse(macroFile, value);
    }
}
