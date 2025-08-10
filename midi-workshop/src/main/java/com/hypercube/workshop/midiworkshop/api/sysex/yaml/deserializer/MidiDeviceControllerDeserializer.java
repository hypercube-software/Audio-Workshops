package com.hypercube.workshop.midiworkshop.api.sysex.yaml.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceController;

import java.io.IOException;

public class MidiDeviceControllerDeserializer extends StdDeserializer<MidiDeviceController> {

    public MidiDeviceControllerDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public MidiDeviceController deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        return MidiDeviceController.of(value);
    }
}
