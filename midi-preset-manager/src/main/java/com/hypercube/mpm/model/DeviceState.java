package com.hypercube.mpm.model;

import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceState {
    private String currentMode;
    private String currentBank;
    private String currentPatch;
    private MidiOutDevice midiOutDevice;
}
