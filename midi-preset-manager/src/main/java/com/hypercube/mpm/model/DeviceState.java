package com.hypercube.mpm.model;

import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DeviceState {
    private String deviceName;
    private String currentMode;
    private String currentBank;
    private Patch currentPatch;
    private List<Patch> currentSearchOutput;
    private List<Integer> currentSelectedCategories = new ArrayList<>();
    private MidiOutDevice midiOutDevice;
}
