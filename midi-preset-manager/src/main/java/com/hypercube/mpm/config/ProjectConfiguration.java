package com.hypercube.mpm.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class ProjectConfiguration {
    private Map<String, SelectedPatch> selectedPatches = new HashMap<>();
    @JsonIgnore
    private MidiDeviceLibrary midiDeviceLibrary;
    @JsonIgnore
    private MidiDeviceManager midiDeviceManager;
}
