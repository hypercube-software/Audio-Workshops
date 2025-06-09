package com.hypercube.workshop.midiworkshop.common.sysex.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDevicePreset;

import java.io.IOException;
import java.util.Map;

public class MidiDevicePresetDeserializer extends StdDeserializer<MidiDevicePreset> {
    private final Map<String, MidiDeviceDefinition> devices;

    public MidiDevicePresetDeserializer(Map<String, MidiDeviceDefinition> devices) {
        super((Class<?>) null);
        this.devices = devices;
    }

    @Override
    public MidiDevicePreset deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        // /deviceModes/PerformanceMode/banks/Performances/presets/.
        MidiDeviceDefinition midiDeviceDefinition = (MidiDeviceDefinition) jsonParser.getParsingContext()
                .getParent() // presets
                .getParent() // Performances
                .getParent() // banks
                .getParent() // PerformanceMode
                .getParent() // deviceModes
                .getCurrentValue();
        MidiDeviceDefinition mainDefinition = devices.get(midiDeviceDefinition.getDeviceName());
        MidiBankFormat midiBankFormat = mainDefinition == null ? midiDeviceDefinition.getPresetFormat() : mainDefinition.getPresetFormat();
        return MidiDevicePreset.of(midiBankFormat, value);
    }
}
