package com.hypercube.mpm.model;

import com.hypercube.mpm.config.SelectedPatch;
import com.hypercube.util.javafx.model.NotObservable;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Getter
@Setter
public class DeviceState {
    private String deviceName;
    private String currentMode;
    private String currentBank;
    private Patch currentPatch;
    private SelectedPatch selectedPatch;
    private List<Patch> currentSearchOutput = new ArrayList<>();
    private List<String> currentSelectedCategories = new ArrayList<>();
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
