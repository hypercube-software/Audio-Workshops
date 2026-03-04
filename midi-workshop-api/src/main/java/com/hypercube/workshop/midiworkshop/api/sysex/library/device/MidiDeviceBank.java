package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetDomain;
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
     * Format of the command
     */
    private MidiBankFormat presetFormat;
    /**
     * Command to retrieve the edit buffer and retrieve the patch name
     * <p>Override the one defined in the mode in {@link MidiDeviceMode#queryName}</p>
     */
    private String queryName;
    /**
     * Optional macro to run before querying name (enter edit mode typically)
     */
    private String preQueryName;
    /**
     * Optional macro to run after querying name (exit edit mode typically)
     */
    private String postQueryName;
    /**
     * Optional, can force the category index for all patches in this bank
     */
    private Integer category;
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
     * Which presets are available in this bank
     */
    private List<MidiDevicePreset> presets = new ArrayList<>();

    public int getZeroBasedChannel() {
        return channel - 1;
    }

    public int getMSB() {
        List<Integer> ids = MidiPresetBuilder.parsePresetSelector(presetFormat, command);
        if (!ids.isEmpty()) {
            return ids.getFirst();
        } else {
            throw new MidiConfigError("Unsupported command format:" + command);
        }
    }

    public int getLSB() {
        List<Integer> ids = MidiPresetBuilder.parsePresetSelector(presetFormat, command);
        if (ids.size() > 1) {
            return ids.get(1);
        } else {
            throw new MidiConfigError("Unsupported command format:" + command);
        }
    }
}
