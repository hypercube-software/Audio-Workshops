package com.hypercube.workshop.midiworkshop.api.sysex.yaml.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;

import java.io.IOException;

public class IntegerDeserializer extends StdDeserializer<Integer> {

    public IntegerDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public Integer deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return MidiEventBuilder.parseNumber(value);
    }
}
