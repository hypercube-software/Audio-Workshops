package com.hypercube.workshop.midiworkshop.common.sysex.yaml.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetDomain;

import java.io.IOException;
import java.util.stream.Collectors;

public class MidiPresetDomainSerializer extends StdSerializer<MidiPresetDomain> {
    public MidiPresetDomainSerializer() {
        super(MidiPresetDomain.class);
    }

    @Override
    public void serialize(MidiPresetDomain midiPresetDomain, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(midiPresetDomain.getRanges()
                .stream()
                .map(r -> "%d-%d".formatted(r.getFrom(), r.getTo()))
                .collect(Collectors.joining(",")));
    }
}
