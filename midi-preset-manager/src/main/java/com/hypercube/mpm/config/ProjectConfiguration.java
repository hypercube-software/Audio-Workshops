package com.hypercube.mpm.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.mpm.model.Patch;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ProjectConfiguration {
    private List<Patch> selectedPatches = new ArrayList<>();
    @JsonIgnore
    private MidiDeviceLibrary midiDeviceLibrary;
    @JsonIgnore
    private MidiDeviceManager midiDeviceManager;
}
