package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetDomain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class MidiDeviceBank {
    private String name;
    private String command;
    private String queryName;
    @Getter(AccessLevel.NONE)
    private int channel = 1; // from [1-16], not [0-15]
    private MidiPresetDomain presetDomain;
    private List<String> presets = new ArrayList<>();

    public int getZeroBasedChannel() {
        return channel - 1;
    }
}
