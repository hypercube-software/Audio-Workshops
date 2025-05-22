package com.hypercube.mpm.config;

import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ProjectConfiguration {
    private List<ProjectDevice> devices;
    private MidiDeviceLibrary midiDeviceLibrary;
    private MidiDeviceManager midiDeviceManager;
}
