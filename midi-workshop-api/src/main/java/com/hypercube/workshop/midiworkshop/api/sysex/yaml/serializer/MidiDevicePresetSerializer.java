package com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;

import java.io.IOException;

public class MidiDevicePresetSerializer extends StdSerializer<MidiDevicePreset> {
    public MidiDevicePresetSerializer() {
        super(MidiDevicePreset.class);
    }

    @Override
    public void serialize(MidiDevicePreset midiDevicePreset, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (midiDevicePreset.drumMap()
                .isEmpty()) {
            String definition = "%s | %s | %s".formatted(midiDevicePreset.command(), midiDevicePreset.category(), midiDevicePreset.name());
            jsonGenerator.writeString(definition);
        } else {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("name", midiDevicePreset.name());
            jsonGenerator.writeStringField("command", midiDevicePreset.command());
            jsonGenerator.writeStringField("category", midiDevicePreset.category());
            jsonGenerator.writeObjectField("drumMap", midiDevicePreset.drumMap());
            jsonGenerator.writeEndObject();
        }
    }
}
