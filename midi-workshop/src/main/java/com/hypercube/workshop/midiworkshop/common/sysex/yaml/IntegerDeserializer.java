package com.hypercube.workshop.midiworkshop.common.sysex.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;

import java.io.IOException;

public class IntegerDeserializer extends StdDeserializer<Integer> {

    public IntegerDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public Integer deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return SysExBuilder.parseNumber(value);
    }
}
