package com.hypercube.midi.translator.config.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.midi.translator.config.lib.MidiDeviceLibrary;
import com.hypercube.midi.translator.config.project.device.DumpRequestTemplate;
import com.hypercube.midi.translator.config.project.device.ProjectDevice;
import com.hypercube.midi.translator.error.ConfigError;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This deserializer rely on {@link MidiDeviceLibrary} to resolve macros on the fly. This mean the returned DumpRequestTemplate does not contain any macro call in its value
 */
public class DumpRequestTemplateDeserializer extends StdDeserializer<DumpRequestTemplate> {
    private static final Pattern hexaNumber = Pattern.compile("(0x|\\$)?(?<value>[0-9A-F]+)");
    private final MidiDeviceLibrary midiDeviceLibrary;

    public DumpRequestTemplateDeserializer(MidiDeviceLibrary midiDeviceLibrary) {
        super((Class<?>) null);
        this.midiDeviceLibrary = midiDeviceLibrary;
    }

    @Override
    public DumpRequestTemplate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String text = jsonParser.getText();
        ProjectDevice projectDevice = (ProjectDevice) jsonParser.getParsingContext()
                .getParent()
                .getCurrentValue();
        // expand the macro if there is one
        text = midiDeviceLibrary.expand(projectDevice.getName(), text);
        // the final string should be "<command name> : <size> : <bytes>"
        String[] values = text.split(":");
        if (values.length != 3) {
            throw new ConfigError("Unexpected Translation definition: " + text);
        } else {
            Integer size = parseOptionalSize(values);
            String name = values[0];
            String value = values.length == 3 ? values[2] : values[1];

            return new DumpRequestTemplate(name
                    .trim(), value
                    .trim(), size);
        }
    }

    private Integer parseOptionalSize(String[] values) {
        Integer size = null;
        Matcher m = hexaNumber.matcher(values[1]);
        if (m.find()) {
            size = Integer.parseInt(m.group("value")
                    .trim(), 16);
        }
        return size;
    }
}
