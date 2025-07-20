package com.hypercube.workshop.midiworkshop.common.sysex.yaml.mixin;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceBank;

import java.util.Map;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public class MidiDeviceModeMixin {
    @JsonProperty("banks")
    private Map<String, MidiDeviceBank> banks;
}
