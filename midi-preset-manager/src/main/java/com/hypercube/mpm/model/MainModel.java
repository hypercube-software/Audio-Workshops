package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.ModelHelper;
import com.hypercube.util.javafx.model.NotObservable;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MainModel {
    private static MainModel observableInstance = null;
    private List<String> devices = new ArrayList<>();
    private List<String> midiInPorts = new ArrayList<>();
    private List<String> selectedInputPorts = new ArrayList<>();
    private List<String> midiThruPorts = new ArrayList<>();
    private List<String> selectedOutputPorts = new ArrayList<>();

    private List<String> deviceModes = new ArrayList<>();
    private List<MidiPresetCategory> modeCategories = new ArrayList<>();
    private List<Integer> modeChannels = new ArrayList<>();
    private List<String> modeBanks = new ArrayList<>();
    private DeviceState currentDeviceState;
    private String currentPatchNameFilter;
    private int currentPatchScoreFilter;
    private String selectedDevice;

    @NotObservable
    private Map<DeviceStateId, DeviceState> deviceStates = new HashMap<>();
    /**
     * Gives some info about the current search output
     */
    private String info;
    /**
     * Gives some info about the latest MIDI event received
     */
    private String eventInfo;

    public static MainModel getObservableInstance() {
        if (observableInstance == null) {
            observableInstance = ModelHelper.forgeMMVM(new MainModel());
        }
        return observableInstance;
    }
}
