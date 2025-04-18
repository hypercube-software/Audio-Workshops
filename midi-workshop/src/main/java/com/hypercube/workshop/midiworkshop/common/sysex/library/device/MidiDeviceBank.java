package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetDomain;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class MidiDeviceBank {
    private String name;
    private String command;
    private MidiPresetDomain presetDomain;
    private List<String> presets = new ArrayList<>();
}
