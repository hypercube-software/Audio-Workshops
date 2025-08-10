package com.hypercube.workshop.midiworkshop.api.sysex.yaml.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;

import java.io.IOException;

public class MidiPresetCategoryDeserializer extends StdDeserializer<MidiPresetCategory> {

    public MidiPresetCategoryDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public MidiPresetCategory deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return MidiPresetCategory.of(value);
    }
}
