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
     * Name of the command
     */
    private String name;
    /**
     * Command to activate the bank in the device
     */
    private String command;
    /**
     * Command to retrieve the edit buffer and retrieve the patch name
     * <p>Override the one defined in the mode in {@link MidiDeviceMode#queryName}</p>
     */
    private String queryName;
    /**
     * We currently do not support multichannel yet
     */
    @Getter(AccessLevel.NONE)
    private int channel = 1; // from [1-16], not [0-15]
    /**
     * Which programs are available in this command
     */
    private MidiPresetDomain presetDomain;
    /**
     * Which presets are available in this command
     */
    private List<MidiDevicePreset> presets = new ArrayList<>();

    public int getZeroBasedChannel() {
        return channel - 1;
    }
}
