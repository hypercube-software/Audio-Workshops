package com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;

import java.io.IOException;

public class MidiPresetCategorySerializer extends StdSerializer<MidiPresetCategory> {
    public MidiPresetCategorySerializer() {
        super(MidiPresetCategory.class);
    }

    @Override
    public void serialize(MidiPresetCategory midiPresetCategory, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(midiPresetCategory.name());
    }
}
