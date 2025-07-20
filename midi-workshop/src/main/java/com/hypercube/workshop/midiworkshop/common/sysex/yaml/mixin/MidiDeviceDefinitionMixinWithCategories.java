package com.hypercube.workshop.midiworkshop.common.sysex.yaml.mixin;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;

import java.util.List;
import java.util.Map;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public class MidiDeviceDefinitionMixinWithCategories {
    @JsonProperty("deviceName")
    private String deviceName;
    @JsonProperty("brand")
    private String brand;
    @JsonProperty("deviceModes")
    private Map<String, MidiDeviceMode> deviceModes;
    @JsonProperty("categories")
    private List<MidiPresetCategory> categories;
}
