package com.hypercube.midi.translator.config.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;

import java.io.IOException;

public class CommandMacroDeserializer extends StdDeserializer<CommandMacro> {
    public CommandMacroDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public CommandMacro deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return CommandMacro.parse(value);
    }
}
