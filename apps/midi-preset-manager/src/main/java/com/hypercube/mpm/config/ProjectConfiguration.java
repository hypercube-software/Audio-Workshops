package com.hypercube.mpm.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.mpm.model.DeviceState;
import com.hypercube.mpm.model.DeviceStateId;
import com.hypercube.mpm.model.Patch;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public class ProjectConfiguration {
    private String selectedOutput;
    private List<String> selectedInputs = new ArrayList<>();
    private List<String> selectedSecondaryOutputs = new ArrayList<>();

    private List<Patch> selectedPatches = new ArrayList<>();
    @JsonIgnore
    private MidiDeviceLibrary midiDeviceLibrary;
    @JsonIgnore
    private MidiPortsManager midiPortsManager;

    /**
     * If we select a patch from a specific mode whereas another patch was selected in a different mode, then the old one is removed
     */
    public void addSelectedPatchToConfig(DeviceStateId stateId, Patch selectedPatch, String selectedPatchDevice, String selectedPatchMode) {
        final List<Patch> list = getSelectedPatches()
                .stream()
                .filter(sp -> (!sp.getDevice()
                        .equals(selectedPatchDevice)) || (
                        sp.getMode()
                                .equals(selectedPatchMode) && !sp.getDeviceStateId()
                                .equals(stateId))
                )
                .collect(Collectors.toList());
        if (selectedPatch != null) {
            list.add(selectedPatch);
        }
        updateAndSortSelectedPatches(list);
    }

    public void removeSelectedPatches(List<DeviceState> removedStates) {
        final List<Patch> toRemove = removedStates.stream()
                .map(DeviceState::getCurrentPatch)
                .toList();
        final List<Patch> list = getSelectedPatches()
                .stream()
                .filter(sp -> !toRemove.contains(sp))
                .collect(Collectors.toList());
        updateAndSortSelectedPatches(list);
    }

    /**
     * In this way we keep the Yaml config nice to read
     */
    private void updateAndSortSelectedPatches(List<Patch> list) {
        setSelectedPatches(list.stream()
                .sorted(Comparator.comparing(Patch::getDevice)
                        .thenComparing(Patch::getMode)
                        .thenComparing(Patch::getChannel))
                .toList());
    }
}
