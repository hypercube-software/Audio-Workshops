package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MidiDeviceMode {
    /**
     * Name of the mode, typically "multi mode" or "voice mode"
     */
    private String name;
    /**
     * How to activate the mode, typically a macro name
     */
    private String command;
    /**
     * How to get the patch name for this mode, typically a macro name
     */
    private String queryName;
    /**
     * Banks belonging to this mode
     */
    private Map<String, MidiDeviceBank> banks = new HashMap<>();
    /**
     * A specific mode can use its own categories (This can be found in Korg devices)
     */
    private List<MidiPresetCategory> categories = new ArrayList<>();
}
