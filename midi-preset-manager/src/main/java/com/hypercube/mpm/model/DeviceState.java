package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.NotObservable;
import com.hypercube.workshop.midiworkshop.api.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Getter
@Setter
public class DeviceState {
    private DeviceStateId id = new DeviceStateId();
    private String currentBank;
    private Patch currentPatch;
    private List<Patch> currentSearchOutput = new ArrayList<>();
    private List<MidiPresetCategory> currentSelectedCategories = new ArrayList<>();
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
