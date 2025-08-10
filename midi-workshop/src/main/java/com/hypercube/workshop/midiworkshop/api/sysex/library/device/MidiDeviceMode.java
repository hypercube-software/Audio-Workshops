package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetNaming;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.IntStream;

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
     * Optional macro to run before querying name (enter edit mode typically)
     */
    private String preQueryName;
    /**
     * Optional macro to run after querying name (exit edit mode typically)
     */
    private String postQueryName;
    /**
     * Override the default naming for the entire device
     */
    private MidiPresetNaming presetNaming;
    /**
     * Banks belonging to this mode
     */
    private Map<String, MidiDeviceBank> banks = new HashMap<>();
    /**
     * A specific mode can use its own categories (This can be found in Korg devices)
     */
    private List<MidiPresetCategory> categories = new ArrayList<>();
    /**
     * Contains a range specifying which midi channels can be used in this mode
     * <p>0-15</p>
     */
    private String midiChannels = "0-15";

    public Optional<MidiDeviceBank> getBank(String name) {
        return Optional.ofNullable(banks.get(name));
    }

    public List<Integer> getChannels() {
        String[] v = midiChannels.split("-");
        int start = Integer.parseInt(v[0]);
        int end = v.length == 2 ? Integer.parseInt(v[1]) : start;
        return IntStream.rangeClosed(start, end)
                .boxed()
                .toList();
    }
}
