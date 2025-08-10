package com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetDomain;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;

import java.util.List;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public class MidiDeviceBankMixinWithCommand {
    @JsonProperty("presets")
    private List<MidiDevicePreset> presets;
    @JsonProperty("command")
    private String command;
    @JsonProperty("presetDomain")
    private MidiPresetDomain presetDomain;
}
