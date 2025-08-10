package com.hypercube.workshop.midiworkshop.api.sysex.yaml.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;

import java.io.File;
import java.io.IOException;

public class CommandMacroDeserializer extends StdDeserializer<CommandMacro> {
    private final File definitionFile;

    public CommandMacroDeserializer(File definitionFile) {
        super((Class<?>) null);
        this.definitionFile = definitionFile;
    }

    @Override
    public CommandMacro deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return CommandMacro.parse(definitionFile, value);
    }
}
