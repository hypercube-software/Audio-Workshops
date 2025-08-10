package com.hypercube.mpm.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.mpm.model.Patch;
import com.hypercube.workshop.midiworkshop.api.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ProjectConfiguration {
    private String selectedOutput;
    private List<String> selectedInputs = new ArrayList<>();
    private List<String> selectedSecondaryOutputs = new ArrayList<>();

    private List<Patch> selectedPatches = new ArrayList<>();
    @JsonIgnore
    private MidiDeviceLibrary midiDeviceLibrary;
    @JsonIgnore
    private MidiDeviceManager midiDeviceManager;
}
