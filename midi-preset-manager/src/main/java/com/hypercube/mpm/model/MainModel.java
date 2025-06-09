package com.hypercube.mpm.model;

import com.hypercube.util.javafx.model.ModelHelper;
import com.hypercube.util.javafx.model.NotObservable;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MainModel {
    private List<String> devices = new ArrayList<>();
    private List<String> deviceModes = new ArrayList<>();
    private List<MidiPresetCategory> modeCategories = new ArrayList<>();
    private List<String> modeBanks = new ArrayList<>();
    private DeviceState currentDeviceState = new DeviceState();
    private String currentPatchNameFilter;
    private int currentPatchScoreFilter;

    @NotObservable
    private Map<String, DeviceState> deviceStates = new HashMap<>();
    private String info;

    private static MainModel observableInstance = null;

    public static MainModel getObservableInstance() {
        if (observableInstance == null) {
            observableInstance = ModelHelper.forgeMMVM(new MainModel());
        }
        return observableInstance;
    }
}
