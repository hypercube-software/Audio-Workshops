package com.hypercube.workshop.midiworkshop.api.sysex.yaml.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MidiDevicePresetDeserializer extends StdDeserializer<MidiDevicePreset> {
    private final Map<String, MidiDeviceDefinition> devices;
    private final File definitionFile;

    public MidiDevicePresetDeserializer(Map<String, MidiDeviceDefinition> devices, File definitionFile) {
        super((Class<?>) null);
        this.devices = devices;
        this.definitionFile = definitionFile;
    }

    @Override
    public MidiDevicePreset deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // /deviceModes/<MODE>/banks/<BANK>/presets/.
        MidiDeviceDefinition device = findInstance(MidiDeviceDefinition.class, jsonParser);
        MidiDeviceDefinition mainDeviceDefinition = devices.containsKey(device.getDeviceName()) ? devices.get(device.getDeviceName()) : device;
        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING) {
            // short spec
            String value = jsonParser.getText();
            return MidiDevicePreset.of(definitionFile, mainDeviceDefinition.getPresetFormat(), value);
        } else if (jsonParser.currentToken() == JsonToken.START_OBJECT) {
            // complete spec
            String command = null;
            String category = null;
            String name = null;
            List<String> drumMap = Collections.emptyList();

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.currentName();
                if (fieldName != null) {
                    jsonParser.nextToken();
                    switch (fieldName) {
                        case "command":
                            command = jsonParser.getText();
                            break;
                        case "category":
                            category = jsonParser.getText();
                            break;
                        case "name":
                            name = jsonParser.getText();
                            break;
                        case "drumMap":
                            drumMap = jsonParser.readValueAs(new TypeReference<List<String>>() {
                            });
                            break;
                        default:
                            jsonParser.skipChildren();
                            break;
                    }
                }
            }
            return new MidiDevicePreset(definitionFile, name, command, category, null, drumMap);
        }

        throw new MidiError("Wrong ConfigMidiPreset definition at line " + jsonParser.currentLocation()
                .getLineNr());

    }

    private <T> T findInstance(Class<T> clazz, JsonParser jsonParser) {
        for (JsonStreamContext ctx = jsonParser.getParsingContext(); ctx != null; ctx = ctx.getParent()) {
            Object instance = ctx.getCurrentValue();
            if (instance != null && instance.getClass()
                    .equals(clazz)) {
                return (T) instance;
            }
        }
        return null;
    }
}
