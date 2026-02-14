package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.NotObservable;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Getter
@Setter
public class DeviceState {
    /**
     * Identity of a state include the device name , the device mode and a MIDI channel
     */
    private DeviceStateId id = new DeviceStateId();
    /**
     * Which patch is selected on this mode and MIDI channel
     */
    private Patch currentPatch;
    /**
     * Banks for this mode
     */
    private List<String> selectedBankNames = new ArrayList<>();
    /**
     * Available patches for this mode
     */
    private List<Patch> currentSearchOutput = new ArrayList<>();
    /**
     * Categories for this mode
     */
    private List<MidiPresetCategory> currentSelectedCategories = new ArrayList<>();
    /**
     * Used when the user go back to a device, he wants to continue where he was (especially the channel)
     */
    private boolean lastUsed;
    
    @NotObservable
    private MidiOutDevice midiOutDevice;

    public int getPatchIndex() {
        if (currentSearchOutput == null || currentPatch == null) {
            return -1;
        } else {
            return IntStream.range(0, currentSearchOutput.size())
                    .filter(i -> currentSearchOutput.get(i)
                            .equals(currentPatch))
                    .findFirst()
                    .orElse(-1);
        }
    }
}
