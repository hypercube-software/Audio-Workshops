package com.hypercube.mpm.model;

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
    private String currentPatchName;
    private List<Patch> currentSearchOutput;
    private List<Integer> currentSelectedCategories = new ArrayList<>();
    private MidiOutDevice midiOutDevice;

    public int getPatchIndex() {
        if (currentSearchOutput == null || currentPatchName == null) {
            return -1;
        } else {
            return IntStream.range(0, currentSearchOutput.size())
                    .filter(i -> currentSearchOutput.get(i)
                            .getName()
                            .equals(currentPatchName))
                    .findFirst()
                    .orElse(-1);
        }
    }
}
