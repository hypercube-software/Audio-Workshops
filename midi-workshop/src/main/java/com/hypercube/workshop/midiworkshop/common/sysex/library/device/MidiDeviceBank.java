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
    /**
     * Name of the bank
     */
    private String name;
    /**
     * Command to activate the bank in the device
     */
    private String command;
    /**
     * Command to retreive the edit buffer and retreive the patch name
     */
    private String queryName;
    /**
     * We currently do not support multichannel yet
     */
    @Getter(AccessLevel.NONE)
    private int channel = 1; // from [1-16], not [0-15]
    /**
     * Which programs are available in this bank
     */
    private MidiPresetDomain presetDomain;
    /**
     * Which presets are available in this bank
     */
    private List<String> presets = new ArrayList<>();

    public int getZeroBasedChannel() {
        return channel - 1;
    }
}
