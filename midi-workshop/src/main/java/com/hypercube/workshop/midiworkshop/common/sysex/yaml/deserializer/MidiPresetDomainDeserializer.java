package com.hypercube.workshop.midiworkshop.common.sysex.yaml.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetDomain;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;

import java.io.File;
import java.io.IOException;

public class MidiPresetDomainDeserializer extends StdDeserializer<MidiPresetDomain> {
    private final File definitionFile;

    public MidiPresetDomainDeserializer(File definitionFile) {
        super((Class<?>) null);
        this.definitionFile = definitionFile;
    }

    @Override
    public MidiPresetDomain deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        // expand the macro if there is one
        MidiDeviceDefinition midiDeviceDefinition = (MidiDeviceDefinition) jsonParser.getParsingContext()
                .getParent()
                .getCurrentValue();

        return MidiPresetDomain.parse(definitionFile, midiDeviceDefinition, value);
    }
}
