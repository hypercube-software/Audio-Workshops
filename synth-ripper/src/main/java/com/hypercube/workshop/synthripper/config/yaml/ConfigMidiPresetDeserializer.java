package com.hypercube.workshop.synthripper.config.yaml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.synthripper.config.presets.ConfigMidiPreset;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Accept two kind of declaration:
 * <ul>
 *     <li>Long definition in multiple lines of YAML (regular spec of a ConfigMidiPreset)</li>
 *     <li>Short definition in a single line string (short spec)</li>
 * </ul>
 */
@Slf4j
public class ConfigMidiPresetDeserializer extends StdDeserializer<IConfigMidiPreset> {
    protected ConfigMidiPresetDeserializer(Class<?> vc) {
        super(vc);
    }

    public ConfigMidiPresetDeserializer() {
        this(null);
    }

    @Override
    public IConfigMidiPreset deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING) {
            // short spec
            return ConfigMidiPreset.fromShortSpec(jsonParser.readValueAs(String.class));
        } else if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
            // long spec
            return jsonParser.readValueAs(ConfigMidiPreset.class);
        }
        throw new MidiError("Wrong ConfigMidiPreset definition at line " + jsonParser.currentLocation()
                .getLineNr());
    }
}
